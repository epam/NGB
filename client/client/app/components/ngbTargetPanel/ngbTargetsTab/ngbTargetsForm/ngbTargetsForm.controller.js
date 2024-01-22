export default class ngbTargetsFormController {

    geneFile = null;

    static get UID() {
        return 'ngbTargetsFormController';
    }

    constructor($scope, $timeout, dispatcher, $mdDialog, ngbTargetsFormService, ngbTargetsTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, $mdDialog, ngbTargetsFormService, ngbTargetsTabService});
    }

    get loading() {
        return this.ngbTargetsFormService.loading;
    }
    set loading(value) {
        this.ngbTargetsFormService.loading = value;
    }
    get failed() {
        return this.ngbTargetsFormService.failed;
    }
    get errorMessageList() {
        return this.ngbTargetsFormService.errorMessageList;
    }
    get launchLoading() {
        return this.ngbTargetsTabService.launchLoading;
    }
    get targetModel() {
        return this.ngbTargetsFormService.targetModel;
    }
    get isAddMode() {
        return this.ngbTargetsTabService.isAddMode;
    }
    get isEditMode() {
        return this.ngbTargetsTabService.isEditMode;
    }
    get updateForce() {
        return this.ngbTargetsFormService.updateForce;
    }
    set updateForce(value) {
        this.ngbTargetsFormService.updateForce = value;
    }
    get targetType() {
        return this.ngbTargetsFormService.targetType;
    }
    get isParasite() {
        return this.targetModel.type === this.targetType.PARASITE;
    }
    get targetTypeName() {
        return this.targetModel.type.toLowerCase()
    }

    async backToTable() {
        this.ngbTargetsFormService.resetTarget();
        this.ngbTargetsTabService.setTableMode();
        this.dispatcher.emit('show:targets:table');
    }

    isGenesEmpty() {
        let {genes} = this.targetModel;
        if (this.isParasite) {
            genes = [...genes, ...this.ngbTargetsFormService.addedGenes]
        }
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
        return (this.ngbTargetsFormService.targetModelChanged()
            || this.ngbTargetsFormService.targetGenesChanged()
            || this.ngbTargetsFormService.parasiteGenesAdded()
        );
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
        if (!this.isAddMode) {
            return !(this.ngbTargetsFormService.targetModelChanged()
                || this.ngbTargetsFormService.targetGenesChanged()
                || this.ngbTargetsFormService.parasiteGenesAdded()
            );
        }
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
            await this.ngbTargetsFormService.deleteTarget();
            this.$timeout(() => this.$scope.$apply());
        });
    }

    getUpdateParasiteTargetRequest() {
        const {id, name, diseases, products, type} = this.targetModel;
        const request = {
            targetId: id,
            targetName: name,
            type,
            diseases,
            products,
        };
        return request;
    }

    async updateTarget() {
        if (this.isParasite) {
            await this.ngbTargetsFormService.updateParasiteTarget()
                .then(success => {
                    if (success) {
                        this.ngbTargetsTabService.setEditMode();
                    }
                });
        } else {
            await this.ngbTargetsFormService.updateTarget()
                .then((success) => {
                    if (success) {
                        this.ngbTargetsTabService.setEditMode();
                    }
                });
        }
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
        await this.ngbTargetsFormService.postNewTarget(request)
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
            this.ngbTargetsFormService.addNewGene();
            this.dispatcher.emit('target:form:add:gene');
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
