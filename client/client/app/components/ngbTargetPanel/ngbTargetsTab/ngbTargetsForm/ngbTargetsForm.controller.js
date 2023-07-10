const GENES_COLUMNS = ['Gene ID', 'Gene Name', 'Tax ID', 'Species Name', 'Priority'];

const PRIORITY_LIST = [{
    name: 'Low',
    value: 'LOW'
}, {
    name: 'High',
    value: 'HIGH'
}];

export default class ngbTargetsFormController{

    diseaseModel;

    get genesColumns () {
        return GENES_COLUMNS;
    }

    get priorityList() {
        return PRIORITY_LIST;
    }

    static get UID() {
        return 'ngbTargetsFormController';
    }

    constructor($scope, $timeout, dispatcher, $mdDialog, ngbTargetsTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, $mdDialog, ngbTargetsTabService});
    }

    get targetModel() {
        return this.ngbTargetsTabService.targetModel;
    }
    get isEditMode() {
        return this.ngbTargetsTabService.isEditMode;
    }

    get loading() {
        return this.ngbTargetsTabService.formLoading;
    }
    set loading(value) {
        this.ngbTargetsTabService.formLoading = value;
    }
    get failed() {
        return this.ngbTargetsTabService.formFailed;
    }
    get errorMessageList() {
        return this.ngbTargetsTabService.formErrorMessageList;
    }
    get isAddMode() {
        return this.ngbTargetsTabService.isAddMode;
    }

    async backToTable() {
        this.ngbTargetsTabService.resetTarget();
        this.ngbTargetsTabService.setTableMode();
        this.dispatcher.emit('show:targets:table');
    }

    getAddRequest() {
        const {name, diseases, products, genes} = this.targetModel;
        const request = {
            targetName: name,
            diseases,
            products,
            targetGenes: genes.map(gene => {
                if (gene.priority === 'None' || gene.priority === '') {
                    delete gene.priority;
                }
                return gene;
            })
        };
        return request;
    }

    async addTarget() {
        const request = this.getAddRequest();
        await this.ngbTargetsTabService.postNewTarget(request)
            .then((success) => {
                if (success) {
                    this.ngbTargetsTabService.setEditMode();
                }
            });
    }

    getUpdateRequest() {
        const {id, name, diseases, products, genes} = this.targetModel;
        const request = {
            targetId: id,
            targetName: name,
            diseases,
            products,
            targetGenes: genes.map(gene => {
                if (gene.priority === 'None' || gene.priority === '') {
                    delete gene.priority;
                }
                return gene;
            })
        };
        return request;
    }

    async updateTarget() {
        const request = this.getUpdateRequest();
        await this.ngbTargetsTabService.updateTarget(request)
            .then((success) => {
                if (success) {
                    this.ngbTargetsTabService.setEditMode();
                }
            });
    }

    async saveTarget() {
        this.loading = true;
        if (this.isAddMode) {
            await this.addTarget();
        } else if (this.isEditMode) {
            await this.updateTarget();
        }
        this.$timeout(::this.$scope.$apply);
    }

    addGene() {
        this.ngbTargetsTabService.addNewGene();
    }

    isGenesEmpty() {
        const {genes} = this.targetModel;
        const genesEmpty = genes.filter(gene => {
            const {geneId, geneName, taxId, speciesName} = gene;
            return [geneId, geneName, taxId, speciesName].some(field => (
                !field || !String(field).length
            ));
        });
        return genesEmpty.length;
    }

    isAddGeneDisabled() {
        const block = this.targetModel.genes;
        if (!block || !block.length) {
            return false;
        }
        return this.isGenesEmpty();
    }

    isSaveDisabled() {
        if (this.loading) return true;
        const {name, genes} = this.targetModel;
        if (!name || !name.length || !genes || !genes.length) return true;
        if (this.isGenesEmpty()) return true;
        if (!this.isAddMode) return !this.ngbTargetsTabService.targetModelChanged();
    }

    onClickRemove(index) {
        this.targetModel.genes.splice(index, 1);
    }

    removeTarget() {
        this.$mdDialog
            .show({
                template: require('./ngbTargetDeleteDlg.tpl.html'),
                controller: function($scope, $mdDialog, dispatcher) {
                    $scope.delete = function () {
                        dispatcher.emit('target:delete');
                        $mdDialog.hide();
                    };
                    $scope.cancel = function () {
                        $mdDialog.hide();
                    };
                }
            });

        this.dispatcher.on('target:delete', async () => {
            await this.ngbTargetsTabService.deleteTarget();
            this.$timeout(::this.$scope.$apply);
        });
    }
}
