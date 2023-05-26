const GENES_COLUMNS = ['Gene ID', 'Gene Name', 'Tax ID', 'Species Name', 'Priority'];

export default class ngbTargetsFormController{

    diseaseModel;

    get genesColumns () {
        return GENES_COLUMNS;
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
        return this.ngbTargetsTabService.loading;
    }
    set loading(value) {
        this.ngbTargetsTabService.loading = value;
    }
    get failed() {
        return this.ngbTargetsTabService.failed;
    }
    get errorMessageList() {
        return this.ngbTargetsTabService.errorMessageList;
    }
    get isAddMode() {
        return this.ngbTargetsTabService.isAddMode;
    }

    async backToTable() {
        this.ngbTargetsTabService.resetTarget();
        this.ngbTargetsTabService.setTableMode();
        this.dispatcher.emit('show:targets:table');
    }

    async addTarget() {
        const {name, diseases, products, genes} = this.targetModel;
        const request = {
            targetName: name,
            diseases,
            products,
            targetGenes: genes
        };
        await this.ngbTargetsTabService.postNewTarget(request)
            .then((success) => {
                if (success) {
                    this.ngbTargetsTabService.setEditMode();
                }
            });
    }

    async updateTarget() {
        const {id, name, diseases, products, genes} = this.targetModel;
        const request = {
            targetId: id,
            targetName: name,
            diseases,
            products,
            targetGenes: genes
        };
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

    isGeneEmpty(index) {
        const {geneId, geneName, taxId, speciesName} = this.targetModel.genes[index];
        return [geneId, geneName, taxId, speciesName].some(field => !field || !`${field}`.length);
    }

    isAddGeneDisabled() {
        const block = this.targetModel.genes;
        if (!block || !block.length) {
            return false;
        }
        return this.isGeneEmpty(block.length - 1);
    }

    isSaveDisabled() {
        if (this.loading) return true;
        const {name, genes} = this.targetModel;
        if (!name || !name.length || !genes || !genes.length) return true;
        if (this.isAddMode) {
            return this.isGeneEmpty(0);
        } else {
            const genesEmpty = genes.filter((gene, index) => !this.isGeneEmpty(index));
            if (!genesEmpty.length) return true;
            return !this.ngbTargetsTabService.targetModelChanged();
        }
    }

    onClickRemove(index) {
        const block = this.targetModel.genes.filter((el, ind) => ind !== index);
        this.targetModel.genes = [...block];
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
