export default class ngbTargetsFormController {

    geneFile = null;

    static get UID() {
        return 'ngbTargetsFormController';
    }

    constructor($scope, $timeout, dispatcher, $mdDialog, ngbTargetsTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, $mdDialog, ngbTargetsTabService});
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
    get launchLoading() {
        return this.ngbTargetsTabService.launchLoading;
    }
    get targetModel() {
        return this.ngbTargetsTabService.targetModel;
    }
    get isAddMode() {
        return this.ngbTargetsTabService.isAddMode;
    }
    get isEditMode() {
        return this.ngbTargetsTabService.isEditMode;
    }
    get updateForce() {
        return this.ngbTargetsTabService.updateForce;
    }
    set updateForce(value) {
        this.ngbTargetsTabService.updateForce = value;
    }
    get targetType() {
        return this.ngbTargetsTabService.targetType;
    }

    async backToTable() {
        this.ngbTargetsTabService.resetTarget();
        this.ngbTargetsTabService.setTableMode();
        this.dispatcher.emit('show:targets:table');
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

    isIdentifyDisabled() {
        if (this.loading || this.isAddMode) return true;
        const {name, genes} = this.targetModel;
        if (!name || !name.length || !genes || !genes.length) return true;
        if (this.isGenesEmpty()) return true;
        return this.ngbTargetsTabService.targetModelChanged();
    }

    identifyTarget() {
        const {id, name, genes} = this.targetModel;
        if (!id || !name || !genes || !genes.length) return;
        const target = {
            id,
            name,
            species: {
                value: this.targetModel.genes.map(g => ({
                    geneId: g.geneId,
                    geneName: g.geneName,
                    speciesName: g.speciesName,
                    taxId: g.taxId
                }))
            }
        }
        this.dispatcher.emit('target:launch:identification', target);
    }

    isSaveDisabled() {
        if (this.loading) return true;
        const {name, genes} = this.targetModel;
        if (!name || !name.length || !genes || !genes.length) return true;
        if (this.isGenesEmpty()) return true;
        if (!this.isAddMode) return !this.ngbTargetsTabService.targetModelChanged();
    }

    async saveTarget() {
        this.loading = true;
        if (this.isAddMode) {
            await this.addTarget();
        } else if (this.isEditMode) {
            await this.updateTarget();
        }
        this.$timeout(() => this.$scope.$apply());
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
            this.$timeout(() => this.$scope.$apply());
        });
    }

    getUpdateRequest() {
        const {id, name, diseases, products, genes, type} = this.targetModel;
        const request = {
            targetId: id,
            targetName: name,
            type,
            diseases,
            products,
            targetGenes: genes.map(g => {
                const gene = {
                    geneId: g.geneId,
                    geneName: g.geneName,
                    taxId: g.taxId,
                    speciesName: g.speciesName,
                };
                if (g.priority && g.priority !== 'None') {
                    gene.priority = g.priority;
                }
                return gene;
            }),
        };
        if (this.updateForce) {
            request.force = true;
        }
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

    getAddRequest() {
        const {name, diseases, products, genes, type} = this.targetModel;
        const request = {
            targetName: name,
            type,
            diseases,
            products,
            targetGenes: genes.map(g => {
                const gene = {
                    geneId: g.geneId,
                    geneName: g.geneName,
                    taxId: g.taxId,
                    speciesName: g.speciesName,
                };
                if (g.priority && g.priority !== 'None') {
                    gene.priority = g.priority;
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

    get isAddGeneDisabled () {
        if (this.loading) return true;
        const block = this.targetModel.genes;
        if (!block || !block.length) {
            return false;
        }
        return this.isGenesEmpty();
    }

    addGene() {
        if (!this.isAddGeneDisabled) {
            this.ngbTargetsTabService.addNewGene();
        }
    }

    uploadGenes() {
        console.log('uploadGenes')
    }

    importGenes() {
        console.log(this.geneFile)
    }

    cancelImport() {
        this.geneFile = null;
    }
}
