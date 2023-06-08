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

export default class ngbTargetsTabService {

    _targetMode = this.mode.TABLE;
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

    get mode () {
        return MODE;
    }

    get isTableMode() {
        return this._targetMode === this.mode.TABLE;
    }
    get isAddMode() {
        return this._targetMode === this.mode.ADD;
    }
    get isEditMode() {
        return this._targetMode === this.mode.EDIT;
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

    static instance (dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbTargetsTabService(dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbTargetPanelService, targetDataService});
    }

    setTableMode() {
        this._targetMode = this.mode.TABLE;
    }
    setAddMode() {
        this._targetMode = this.mode.ADD;
    }
    setEditMode() {
        this._targetMode = this.mode.EDIT;
    }
    addNewGene() {
        this._targetModel.genes.push({...NEW_GENE});
    }

    setTargetModel(data) {
        this._targetModel = {
            id: data.targetId,
            name: data.targetName,
            genes: data.targetGenes.map(gene => ({
                geneId: gene.geneId,
                geneName: gene.geneName,
                taxId: gene.taxId,
                speciesName: gene.speciesName,
                priority: gene.priority
            })),
            diseases: (data.diseases || []).filter(d => d),
            products: (data.products || []).filter(p => p)
        };
    }

    get originalModel() {
        return this._originalModel;
    }
    set originalModel(value) {
        this._originalModel = value;
        this._originalModel.diseases = (this._originalModel.diseases || []).filter(d => d);
        this._originalModel.products = (this._originalModel.products || []).filter(p => p);
    }

    getTarget(id) {
        return new Promise(resolve => {
            this.targetDataService.getTargetById(id)
                .then(data => {
                    this.formFailed = false;
                    this.formErrorMessageList = null;
                    this.setTargetModel(data);
                    this.originalModel = data;
                    this.setEditMode();
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
            products: []
        };
    }

    resetTarget() {
        this.setEmptyTargetModel();
        this._loading = false;
        this._failed = false;
        this._errorMessageList = null;
    }

    targetModelChanged() {
        const nameChanged = this.originalModel.targetName !== this.targetModel.name;
        const changed = (block) => {
            const originalBlock = (this.originalModel[block] || []).sort();
            const modelBlock = (this.targetModel[block] || []).sort();
            if (originalBlock.length !== modelBlock.length) return true;
            return originalBlock.some((item, index) => (
                item !== modelBlock[index]
            ));
        };
        const genesChanged = () => {
            if (this.originalModel.targetGenes.length !== this.targetModel.genes.length) {
                return true;
            }
            return this.targetModel.genes.some((gene, index) => (
                Object.entries(gene).some(([key, value]) => (
                    value !== this.originalModel.targetGenes[index][key]
                ))
            ));
        };
        return nameChanged || changed('diseases') || changed('products') || genesChanged();
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

    searchGenes(geneId) {
        return new Promise(resolve => {
            this.targetDataService.searchGenes(geneId)
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

    setGeneModel(index, field, value, isSelected) {
        const geneFields = {
            featureId: 'geneId',
            featureName: 'geneName',
            priority: 'priority'
        };
        if (geneFields[field]) {
            this._targetModel.genes[index][geneFields[field]] = value;
            if (isSelected) {
                this._targetModel.genes[index].taxId = 9606;
                this._targetModel.genes[index].speciesName = 'Homo sapiens';
            }
        }
        this.dispatcher.emit('gene:model:updated');
    }

    selectedGeneChanged(gene, index) {
        if (gene) {
            for (const [key, value] of Object.entries(gene)) {
                this.setGeneModel(index, key, value, true);
            }
        }
    }

    async getIdentificationData(params, info) {
        this._launchLoading = true;
        await this.launchTargetIdentification(params)
            .then(result => {
                this._launchLoading = false;
                if (result) {
                    this.dispatcher.emit('target:launch:finished', result, info);
                } else {
                    this.dispatcher.emit('target:launch:failed');
                }
            });
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
}
