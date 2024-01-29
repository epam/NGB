export default class ngbTargetsFormController {

    static get UID() {
        return 'ngbTargetsFormController';
    }

    constructor($scope, $timeout, dispatcher, $mdDialog, ngbTargetsFormService, ngbTargetsTabService, ngbTargetGenesTableService) {
        Object.assign(this, {$scope, $timeout, dispatcher, $mdDialog, ngbTargetsFormService, ngbTargetsTabService, ngbTargetGenesTableService});
        dispatcher.on('target:form:changes:save', this.updateTarget.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:form:changes:save', this.updateTarget.bind(this));
        });
        this.geneFile = null;
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
    get originalModel() {
        return this.ngbTargetsFormService.originalModel;
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

    get geneFile() {
        return this.ngbTargetsFormService.geneFile;
    }
    set geneFile(value) {
        this.ngbTargetsFormService.geneFile = value;
    }

    async backToTable() {
        this.ngbTargetsFormService.resetTarget();
        this.ngbTargetsTabService.setTableMode();
        this.dispatcher.emit('show:targets:table');
    }


    isIdentifyDisabled() {
        if (this.loading || this.isAddMode) return true;
        const {name, genes} = this.targetModel;
        if (!name || !name.length || !genes || !genes.length) return true;
        if (this.ngbTargetsFormService.isSomeGeneEmpty()) return true;
        return (this.ngbTargetsFormService.targetInfoChanged()
            || this.ngbTargetsFormService.targetGenesChanged()
            || this.ngbTargetsFormService.parasiteGenesAdded()
            || this.geneFile
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
        const {name} = this.targetModel;
        if (!name || !name.length) return true;
        if (this.ngbTargetsFormService.areGenesEmpty()) {
            if (!(this.isParasite && this.geneFile)) {
                return true;
            }
        }
        if (this.ngbTargetsFormService.isSomeGeneEmpty()) return true;
        if (!this.isAddMode) {
            return !(this.ngbTargetsFormService.targetInfoChanged()
                || this.ngbTargetsFormService.targetGenesChanged()
                || this.ngbTargetsFormService.parasiteGenesAdded()
                || this.geneFile
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

    async addTarget() {
        if (this.isParasite) {
            await this.ngbTargetsFormService.postNewParasiteTarget()
                .then(success => {
                    if (success) {
                        this.ngbTargetsTabService.setEditMode();
                        this.dispatcher.emit('target:form:saved');
                        this.inputFile = undefined;
                        this.$timeout(() => this.$scope.$apply());
                    }
                });
        } else {
            await this.ngbTargetsFormService.postNewTarget()
                .then((success) => {
                    if (success) {
                        this.ngbTargetsTabService.setEditMode();
                    }
                });
        }
    }

    async updateTarget(addGene) {
        const geneWasChanged = this.ngbTargetsFormService.needSaveGeneChanges();
        if (this.isParasite) {
            const updateFilterList = geneWasChanged ? this.ngbTargetsFormService.getChangedFields() : false;
            await this.ngbTargetsFormService.updateParasiteTarget()
                .then(success => {
                    if (success) {
                        this.ngbTargetsTabService.setEditMode();
                        this.dispatcher.emit('target:form:saved');
                        if (addGene) {
                            this.ngbTargetsFormService.addNewGene();
                        }
                        if (updateFilterList && updateFilterList.length) {
                            updateFilterList.map(field => this.ngbTargetGenesTableService.setFilterList(field));
                        }
                        this.inputFile = undefined;
                        this.$timeout(() => this.$scope.$apply());
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

    get isAddGeneDisabled () {
        if (this.loading) return true;
        const block = this.isParasite ? this.addedGenes : this.targetModel.genes;
        if (!block || !block.length) {
            return false;
        }
        return this.ngbTargetsFormService.isSomeGeneEmpty();
    }

    addGene() {
        if (!this.isAddGeneDisabled) {
            const model = this.isAddMode ? this.targetModel : this.originalModel;
            if (model.type === this.targetType.PARASITE) {
                if (this.isAddMode) {
                    this.ngbTargetsFormService.addNewGene();
                } else {
                    this.dispatcher.emit('target:form:add:gene');
                }
            } else if (model.type === this.targetType.DEFAULT) {
                this.ngbTargetsFormService.addNewGene();
            }
        }
    }

    async uploadFile() {
        const input = document.getElementById('target-genes-file-input');
        if (input.files.length > 0) {
            this.geneFile = input.files[0];
        }
    }

    cancelImport() {
        if (this.loading) return;
        this.inputFile = undefined;
        this.geneFile = null;
    }

    get addedGenes() {
        return this.ngbTargetsFormService.addedGenes;
    }
    set addedGenes(value) {
        this.ngbTargetsFormService.addedGenes = value;
    }

    onChangeType() {
        if (this.targetModel.type === this.targetType.DEFAULT) {
            this.targetModel.genes = [...this.addedGenes];
            this.addedGenes = [];
            this.geneFile = null;
            this.inputFile = undefined;
        }
        if (this.targetModel.type === this.targetType.PARASITE) {
            this.addedGenes = [...this.targetModel.genes];
            this.targetModel.genes = [];
        }
        this.dispatcher.emit('target:model:type:changed');
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
}
