const NEW_GENE = {
    geneId: '',
    geneName: '',
    taxId: '',
    speciesName: '',
    priority: ''
};

const GENE_MODEL_PROPERTIES = ['geneId', 'geneName', 'taxId', 'speciesName', 'priority'];

export default class ngbTargetsFormService {

    get geneModelProperties() {
        return GENE_MODEL_PROPERTIES;
    }

    _targetModel;
    _originalModel;
    _updateForce = false;
    _addedGenes = [];

    _loading = false;
    _failed = false;
    _errorMessageList = null;

    get loading() {
        return this._loading;
    }
    set loading(value) {
        this._loading = value;
    }
    get failed () {
        return this._failed;
    }
    set failed (value) {
        this._failed = value;
    }
    get errorMessageList() {
        return this._errorMessageList;
    }
    set errorMessageList(value) {
        this._errorMessageList = value;
    }
    get updateForce() {
        return this._updateForce;
    }
    set updateForce(value) {
        this._updateForce = value;
    }
    get addedGenes() {
        return this._addedGenes;
    }
    set addedGenes(value) {
        this._addedGenes = value;
    }

    get targetModel() {
        if (this.isAddMode && !this._targetModel) {
            this.setEmptyTargetModel();
        }
        return this._targetModel;
    }

    get originalModel() {
        return this._originalModel;
    }
    set originalModel(value) {
        this._originalModel = value;
        this._originalModel.diseases = (this._originalModel.diseases || []).filter(d => d);
        this._originalModel.products = (this._originalModel.products || []).filter(p => p);
    }
    get isParasite() {
        return this.targetModel.type === this.targetType.PARASITE;
    }

    static instance (dispatcher, ngbTargetsTabService, targetDataService, targetContext) {
        return new ngbTargetsFormService(dispatcher, ngbTargetsTabService, targetDataService, targetContext);
    }

    constructor(dispatcher, ngbTargetsTabService, targetDataService, targetContext) {
        Object.assign(this, {dispatcher, ngbTargetsTabService, targetDataService, targetContext});
        dispatcher.on('homologs:create:target', this.createTargetFromHomologs.bind(this));
    }

    get isAddMode() {
        return this.ngbTargetsTabService.isAddMode;
    }

    get targetType() {
        return this.targetContext.targetType;
    }

    setEmptyTargetModel() {
        this._targetModel = {
            name: '',
            genes: [],
            diseases: [],
            products: [],
            type: this.targetType.DEFAULT
        };
        this._originalModel = undefined;
        this.dispatcher.emit('target:model:changed');
    }

    parasiteGenesAdded() {
        return this.isParasite && !!this.addedGenes.length;
    }

    targetGenesChanged() {
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
        return nameChanged || typeChanged || changed('diseases') || changed('products');
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
        this.targetContext.targetModelType = data.type;
        this.dispatcher.emit('target:model:changed');
    }

    async getTarget(id) {
        this.resetTarget();
        this.loading = true;
        this.updateForce = false;
        return new Promise(resolve => {
            this.targetDataService.getTargetById(id)
                .then(data => {
                    this.failed = false;
                    this.errorMessageList = null;
                    this.setTargetModel(data);
                    this.originalModel = data;
                    this.ngbTargetsTabService.setEditMode();
                    this.loading = false;
                    resolve(true);
                })
                .catch(err => {
                    this.failed = true;
                    this.errorMessageList = [err.message];
                    this.loading = false;
                    resolve(false);
                });
        });
    }

    setParasiteTargetGenes(genes) {
        this._targetModel.genesTotal = genes.totalCount;
        this._targetModel.genes = (genes.items || []).map(g => ({
            geneId: g.geneId,
            geneName: g.geneName,
            taxId: g.taxId,
            speciesName: g.speciesName,
            priority: g.priority,
            ...g.metadata
        }));
        this.originalModel.targetGenes = genes.items || [];
    }

    getTargetGenes(id, request) {
        return new Promise(resolve => {
            this.targetDataService.getTargetGenesById(id, request)
                .then(genes => {
                    this.failed = false;
                    this.errorMessageList = null;
                    this.setParasiteTargetGenes(genes);
                    this.ngbTargetsTabService.setEditMode();
                    resolve(true)
                })
                .catch(err => {
                    this.failed = true;
                    this.errorMessageList = [err.message];
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
                    this.failed = true;
                    this.errorMessageList = [err.message];
                    resolve([]);
                });
        });
    }

    addNewGene(finished) {
        const model = this.isAddMode ? this.targetModel : this.originalModel;
        if (model.type === this.targetType.PARASITE && finished) {
            this.addedGenes.push({...NEW_GENE});
            this.dispatcher.emit('target:form:gene:added');
        } else if (model.type === this.targetType.DEFAULT) {
            this._targetModel.genes.push({...NEW_GENE});
        }
    }

    postNewTarget(request) {
        return new Promise(resolve => {
            this.targetDataService.postNewTarget(request)
                .then(result => {
                    if (result) {
                        this.failed = false;
                        this.errorMessageList = null;
                        this.setTargetModel(result);
                        this.originalModel = result;
                        this.ngbTargetsTabService.setEditMode();
                    }
                    this.loading = false;
                    resolve(true);
                })
                .catch(err => {
                    this.failed = true;
                    this.errorMessageList = [err.message];
                    this.loading = false;
                    resolve(false);
                });
        });
    }

    getParasiteGenesRequest(genes) {
        const request = genes.map(g => {
            const gene = {
                targetId: this.targetModel.id,
                geneId: g.geneId,
                geneName: g.geneName,
                taxId: g.taxId,
                speciesName: g.speciesName,
                metadata: {}
            };
            if (g.priority && g.priority !== 'None') {
                gene.priority = g.priority;
            }
            return gene;
        });
        return request;
    }


    putParasiteTarget(request) {
        return new Promise((resolve, reject) => {
            this.targetDataService.updateTarget(request)
                .then(result => resolve(result))
                .catch(err => reject(err));
        });
    }

    putParasiteGenes(request) {
        return new Promise((resolve, reject) => {
            this.targetDataService.putTargetGenes(request)
                .then(() => resolve(true))
                .catch(err => reject(err));
        });
    }

    postParasiteGenes() {
        const request = this.getParasiteGenesRequest(this.addedGenes);
        const targetId = this.targetModel.id;
        return new Promise((resolve, reject) => {
            this.targetDataService.postTargetGenes(targetId, request)
                .then(() => resolve(true))
                .catch(err => reject(err));
        });
    }

    getParasiteTargetRequest() {
        const {id, name, diseases, products, type} = this.targetModel;
        const request = {
            targetId: id,
            targetName: name,
            type,
            diseases,
            products,
        };
        if (this.updateForce) {
            request.force = true;
        }
        return request;
    }

    async updateParasiteTarget() {
        const promises = [];
        if (this.parasiteGenesAdded()) {
            promises.push(await this.postParasiteGenes());
        }
        // if (this.targetGenesChanged()) {
        //     const request = this.getParasiteGenesRequest(this.targetModel.genes);
        //     promises.push(await this.putParasiteGenes(request));
        // }
        // if (this.targetModelChanged()) {
        //     const request = this.getParasiteTargetRequest();
        //     promises.push(await this.putParasiteTarget(request));
        // }
        Promise.all(promises).then(values => {
            this.dispatcher.emit('target:form:refreshed', 1);
            return true;
        })
            .catch(err => console.log(err))
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

    updateTarget() {
        const request = this.getUpdateRequest();
        return new Promise(resolve => {
            this.targetDataService.updateTarget(request)
                .then(result => {
                    if (result) {
                        this.failed = false;
                        this.errorMessageList = null;
                        this.setTargetModel(result);
                        this.originalModel = result;
                        this.updateForce = false;
                        this.ngbTargetsTabService.setEditMode();
                    }
                    this.loading = false;
                    resolve(true);
                })
                .catch(err => {
                    this.failed = true;
                    this.errorMessageList = [err.message];
                    this.loading = false;
                    resolve(false);
                });
        });
    }

    deleteTarget() {
        return new Promise((resolve) => {
            this.targetDataService.deleteTarget(this.targetModel.id)
                .then(() => {
                    this.errorMessageList = null;
                    this.failed = false;
                    this.ngbTargetsTabService.setTableMode();
                    resolve();
                })
                .catch(error => {
                    this.errorMessageList = [error.message];
                    this.failed = true;
                    resolve();
                });
        });
    }

    createTargetFromHomologs(info) {
        this.loading = true;
        this.setEmptyTargetModel();
        this.ngbTargetsTabService.setAddMode();
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
            this.loading = false;
        });
    }

    resetTarget() {
        this.setEmptyTargetModel();
        this.loading = false;
        this.failed = false;
        this.errorMessageList = null;
        this.addedGenes = [];
    }

    setGeneModel(row, field, value) {
        const geneFields = {
            geneId: 'geneId',
            geneName: 'geneName',
            priority: 'priority',
            taxId: 'taxId',
            speciesName: 'speciesName'
        };
        const model = this.isAddMode ? this.targetModel : this.originalModel;
        if (model.type === this.targetType.PARASITE) {
            let index = this._targetModel.genes.indexOf(row);
            if (index === -1) {
                let index = this.addedGenes.indexOf(row);
                if (index !== -1 && geneFields[field]) {
                    this.addedGenes[index][geneFields[field]] = value;
                }
            }
        } else {
            const index = this._targetModel.genes.indexOf(row);
            if (index !== -1 && geneFields[field]) {
                this._targetModel.genes[index][geneFields[field]] = value;
            }
        }
    }

    selectedGeneChanged(row, gene) {
        if (gene) {
            for (const [key, value] of Object.entries(gene)) {
                this.setGeneModel(row, key, value);
            }
        }
    }

    setColumnsList() {
        const targetId = this.targetModel.id;
        return new Promise(resolve => {
            this.targetDataService.getTargetGenesFields(targetId)
                .then(columns => resolve(columns))
                .catch(err => resolve([]));
        });
    }
}
