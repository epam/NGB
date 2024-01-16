const MODE = {
    TABLE: 'table',
    ADD: 'add',
    EDIT: 'edit'
};

const NEW_GENE = {
    geneId: '',
    geneName: '',
    taxId: '',
    speciesName: '',
    priority: ''
};

const GENE_MODEL_PROPERTIES = ['geneId', 'geneName', 'taxId', 'speciesName', 'priority'];

export default class ngbTargetsTabService {

    _targetMode;
    _targetModel;
    _originalModel;

    _formLoading = false;
    _formFailed = false;
    _formErrorMessageList = null;

    _tableLoading = false;
    _tableFailed = false;
    _tableErrorMessageList = null;

    _launchLoading = false;
    _launchFailed = false;
    _launchErrorMessageList = null;

    _updateForce = false;

    get mode () {
        return MODE;
    }

    get geneModelProperties() {
        return GENE_MODEL_PROPERTIES;
    }
    get targetType() {
        return this.targetContext.targetType;
    }

    get targetMode() {
        return this._targetMode;
    }
    set targetMode(value) {
        this._targetMode = value;
    }

    get isTableMode() {
        return this.targetMode === this.mode.TABLE;
    }
    get isAddMode() {
        return this.targetMode === this.mode.ADD;
    }
    get isEditMode() {
        return this.targetMode === this.mode.EDIT;
    }
    get formLoading() {
        return this._formLoading;
    }
    set formLoading(value) {
        this._formLoading = value;
    }
    get formFailed() {
        return this._formFailed;
    }
    set formFailed(value) {
        this._formFailed = value;
    }
    get formErrorMessageList() {
        return this._formErrorMessageList;
    }
    set formErrorMessageList(value) {
        this._formErrorMessageList = value;
    }

    get tableLoading() {
        return this._tableLoading;
    }
    set tableLoading(value) {
        this._tableLoading = value;
    }
    get tableFailed() {
        return this._tableFailed;
    }
    set tableFailed(value) {
        this._tableFailed = value;
    }
    get tableErrorMessageList() {
        return this._tableErrorMessageList;
    }
    set tableErrorMessageList(value) {
        this._tableErrorMessageList = value;
    }

    get launchLoading() {
        return this._launchLoading;
    }
    get launchFailed() {
        return this._launchFailed;
    }
    set launchFailed(value) {
        this._launchFailed = value;
    }
    get launchErrorMessageList() {
        return this._launchErrorMessageList;
    }
    set launchErrorMessageList(value) {
        this._launchErrorMessageList = value;
    }

    get targetModel() {
        if (this.isAddMode && !this._targetModel) {
            this.setEmptyTargetModel();
        }
        return this._targetModel;
    }

    get emptyResults () {
        return !this.loadingData &&
            !this.failedResult &&
            (!this.gridOptions || !this.gridOptions.data || this.gridOptions.data.length === 0);
    }

    get updateForce() {
        return this._updateForce;
    }
    set updateForce(value) {
        this._updateForce = value;
    }

    static instance (
        $timeout,
        dispatcher,
        ngbTargetPanelService,
        targetDataService,
        projectContext,
        targetContext,
    ) {
        return new ngbTargetsTabService(
            $timeout,
            dispatcher,
            ngbTargetPanelService,
            targetDataService,
            projectContext,
            targetContext,
        );
    }

    constructor(
        $timeout,
        dispatcher,
        ngbTargetPanelService,
        targetDataService,
        projectContext,
        targetContext,
    ) {
        Object.assign(this, {
            $timeout,
            dispatcher,
            ngbTargetPanelService,
            targetDataService,
            projectContext,
            targetContext,
        });
        dispatcher.on('homologs:create:target', this.createTargetFromHomologs.bind(this));
        this.setTableMode();
    }

    setTableMode() {
        this.targetMode = this.mode.TABLE;
        this.targetContext.setTargetsTableActionsVisibility(this.targetMode);
        this.targetContext.setTargetsFormActionsVisibility(this.targetMode, this.targetModel);
    }
    setAddMode() {
        this.targetMode = this.mode.ADD;
        this.targetContext.setTargetsTableActionsVisibility(this.targetMode);
        this.targetContext.setTargetsFormActionsVisibility(this.targetMode, this.targetModel);
    }
    setEditMode() {
        this.targetMode = this.mode.EDIT;
        this.targetContext.setTargetsTableActionsVisibility(this.targetMode);
        this.targetContext.setTargetsFormActionsVisibility(this.targetMode, this.targetModel);
    }
    addNewGene() {
        this._targetModel.genes.push({...NEW_GENE});
    }

    setTargetModel(data) {
        this._targetModel = {
            id: data.targetId,
            name: data.targetName,
            genes: (data.targetGenes || []).map(gene => ({
                geneId: gene.geneId,
                geneName: gene.geneName,
                taxId: gene.taxId,
                speciesName: gene.speciesName,
                priority: gene.priority
            })),
            diseases: (data.diseases || []).filter(d => d),
            products: (data.products || []).filter(p => p),
            identifications: data.identifications,
            type: data.type,
        };
        this.dispatcher.emit('target:model:changed');
    }

    get originalModel() {
        return this._originalModel;
    }
    set originalModel(value) {
        this._originalModel = value;
        this._originalModel.diseases = (this._originalModel.diseases || []).filter(d => d);
        this._originalModel.products = (this._originalModel.products || []).filter(p => p);
    }

    setParasiteTargetModel(data, genes) {
        data.genesTotal = genes.totalCount;
        data.targetGenes = genes.items;
        this.setTargetModel(data);
        this.originalModel = data;
    }

    async getTarget(id, type) {
        this.formLoading = true;
        if (type === this.targetType.PARASITE) {
            return this.getParasiteTarget(id);
        } else {
            return this.getDefaultTarget(id);
        }
    }

    async getParasiteTarget(id) {
        this.updateForce = false;
        const getTarget = new Promise((resolve, reject) => {
            this.targetDataService.getTargetById(id)
                .then(data => resolve(data))
                .catch(err => reject(err));
        });
        const getGenes = new Promise((resolve, reject) => {
            const request = {
                "page": 1,
                "pageSize": 10
            };
            this.targetDataService.getTargetGenesById(id, request)
                .then(genes => resolve(genes))
                .catch(err => reject(err));
        });

        return Promise.all([getTarget, getGenes])
            .then(values => {
                const [data, genes] = values;
                this.formFailed = false;
                this.formErrorMessageList = null;
                this.setParasiteTargetModel(data, genes);
                this.setEditMode();
                this.formLoading = false;
            })
            .catch(err => {
                this.formFailed = true;
                this.formErrorMessageList = [err.message];
                this.formLoading = false;
            })
    }

    getDefaultTarget(id) {
        this.updateForce = false;
        return new Promise(resolve => {
            this.targetDataService.getTargetById(id)
                .then(data => {
                    this.formFailed = false;
                    this.formErrorMessageList = null;
                    this.setTargetModel(data);
                    this.originalModel = data;
                    this.setEditMode();
                    this.formLoading = false;
                    resolve(data);
                })
                .catch(err => {
                    this.formFailed = true;
                    this.formErrorMessageList = [err.message];
                    this.formLoading = false;
                    resolve(false);
                });
        });
    }

    deleteTarget() {
        return new Promise((resolve) => {
            this.targetDataService.deleteTarget(this.targetModel.id)
                .then(() => {
                    this._errorMessageList = null;
                    this._failed = false;
                    this.setTableMode();
                    resolve();
                })
                .catch(error => {
                    this._errorMessageList = [error.message];
                    this._failed = true;
                    resolve();
                });
        });
    }

    setEmptyTargetModel() {
        this._targetModel = {
            name: '',
            genes: [],
            diseases: [],
            products: [],
            type: this.targetType.DEFAULT
        };
        this.dispatcher.emit('target:model:changed');
    }

    resetTarget() {
        this.setEmptyTargetModel();
        this._loading = false;
        this._failed = false;
        this._errorMessageList = null;
    }

    targetModelChanged() {
        const nameChanged = this.originalModel.targetName !== this.targetModel.name;
        const typeChanged = this.originalModel.type !== this.targetModel.type;
        const changed = (block) => {
            const originalBlock = (this.originalModel[block] || []).sort();
            const modelBlock = (this.targetModel[block] || []).sort();
            if (originalBlock.length !== modelBlock.length) return true;
            return originalBlock.some((item, index) => (
                item !== modelBlock[index]
            ));
        };
        const genesChanged = () => {
            const originalModel = this.originalModel.targetGenes;
            const targetModel = this.targetModel.genes;
            if (originalModel.length !== targetModel.length) {
                return true;
            }
            return targetModel.some((gene, index) => (
                Object.entries(gene).some(([key, value]) => {
                    if (!this.geneModelProperties.includes(key)) return false;
                    return String(value) !== String(originalModel[index][key]);
                })
            ));
        };
        return nameChanged || typeChanged || changed('diseases') || changed('products') || genesChanged();
    }

    postNewTarget(request) {
        return new Promise(resolve => {
            this.targetDataService.postNewTarget(request)
                .then(result => {
                    if (result) {
                        this.formFailed = false;
                        this.formErrorMessageList = null;
                        this.setTargetModel(result);
                        this.originalModel = result;
                        this.setEditMode();
                    }
                    this.formLoading = false;
                    resolve(true);
                })
                .catch(err => {
                    this.formFailed = true;
                    this.formErrorMessageList = [err.message];
                    this.formLoading = false;
                    resolve(false);
                });
        });
    }

    updateTarget(request) {
        return new Promise(resolve => {
            this.targetDataService.updateTarget(request)
                .then(result => {
                    if (result) {
                        this.formFailed = false;
                        this.formErrorMessageList = null;
                        this.setTargetModel(result);
                        this.originalModel = result;
                        this.updateForce = false;
                        this.setEditMode();
                    }
                    this.formLoading = false;
                    resolve(true);
                })
                .catch(err => {
                    this.formFailed = true;
                    this.formErrorMessageList = [err.message];
                    this.formLoading = false;
                    resolve(false);
                });
        });
    }

    searchGenes(prefix) {
        return new Promise(resolve => {
            this.targetDataService.searchGenes(prefix)
                .then(result => {
                    if (result) {
                        resolve(result);
                    }
                })
                .catch(err => {
                    this._failed = true;
                    this._errorMessageList = [err.message];
                    resolve([]);
                });
        });
    }

    setGeneModel(row, field, value) {
        const index = this._targetModel.genes.indexOf(row);
        const geneFields = {
            geneId: 'geneId',
            geneName: 'geneName',
            priority: 'priority',
            taxId: 'taxId',
            speciesName: 'speciesName'
        };
        if (geneFields[field]) {
            this._targetModel.genes[index][geneFields[field]] = value;
        }
    }

    selectedGeneChanged(row, gene) {
        if (gene) {
            for (const [key, value] of Object.entries(gene)) {
                this.setGeneModel(row, key, value);
            }
        }
    }

    async getIdentificationData(params, info) {
        this.ngbTargetPanelService.resetIdentificationData();
        this._launchLoading = true;
        const result = await this.launchTargetIdentification(params)
            .then(result => {
                this._launchLoading = false;
                if (result) {
                    this.dispatcher.emit('target:launch:finished', result, info);
                } else {
                    this.dispatcher.emit('target:launch:failed');
                    this.$timeout(() => {
                        this._launchFailed = false;
                        this._launchErrorMessageList = null;
                        this.dispatcher.emit('target:launch:failed:refresh');
                    }, 5000);
                }
                return result;
            });
        this.setTableMode();
        return result;
    }

    launchTargetIdentification(request) {
        return new Promise(resolve => {
            this.targetDataService.postTargetIdentification(request)
                .then(result => {
                    if (result && result.desription) {
                        this._launchFailed = false;
                        this._launchErrorMessageList = null;
                    }
                    resolve(result);
                })
                .catch(err => {
                    this._launchFailed = true;
                    this._launchErrorMessageList = [err.message];
                    resolve(false);
                });
        });
    }

    createTargetFromHomologs(info) {
        this.formLoading = true;
        this.setEmptyTargetModel();
        this.setAddMode();
        this.$timeout(() => {
            this._targetModel.name = info.targetName;
            this._targetModel.diseases = [];
            this._targetModel.products = [];
            this._targetModel.genes = info.genes.map(g => ({
                geneId: g.geneId,
                geneName: g.geneName,
                taxId: g.taxId,
                speciesName: g.speciesName,
                priority: ''
            }));
            const mdChips = document.getElementsByClassName('chip-input');
            for (let i = 0; i < mdChips.length; i++) {
                const input = mdChips[i].getElementsByClassName('md-input');
                for (let j = 0; j < input.length; j++) {
                    input[j].value = '';
                    input[j].innerHTML = '';
                }
            }
            this.formLoading = false;
        });
    }
}
