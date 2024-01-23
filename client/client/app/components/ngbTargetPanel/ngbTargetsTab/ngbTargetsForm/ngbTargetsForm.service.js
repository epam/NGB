const NEW_GENE = {
    geneId: '',
    geneName: '',
    taxId: '',
    speciesName: '',
    priority: ''
};

const PAGE_SIZE = 20;

const GENE_MODEL_PROPERTIES = ['geneId', 'geneName', 'taxId', 'speciesName', 'priority'];
const DEFAULT_FIELDS = ['Gene ID', 'Gene Name', 'Tax ID', 'Species Name', 'Priority'];

export default class ngbTargetsFormService {

    get geneModelProperties() {
        return GENE_MODEL_PROPERTIES;
    }

    get pageSize() {
        return PAGE_SIZE;
    }
    get defaultFields() {
        return DEFAULT_FIELDS;
    }

    _targetModel;
    _originalModel;
    _updateForce = false;
    _addedGenes = [];
    _removedGenes = [];
    metadataFields = [];

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
    get removedGenes() {
        return this._removedGenes;
    }
    set removedGenes(value) {
        this._removedGenes = value;
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

    parasiteGenesRemoved() {
        return this.removedGenes && this.removedGenes.length;
    }

    targetGenesChanged() {
        const originalGenes = this.originalModel.targetGenes;
        const targetGenes = this.targetModel.genes;
        if (originalGenes.length !== targetGenes.length) {
            return true;
        }
        return targetGenes.some((gene, index) => (
            Object.entries(gene).some(([key, value]) => {
                if (!this.geneModelProperties.includes(key)) return false;
                return String(value) !== String(originalGenes[index][key]);
            })
        ));
    }

    targetInfoChanged() {
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

    isSomeGeneEmpty() {
        let {genes} = this.targetModel;
        if (this.isParasite) {
            genes = [...genes, ...this.addedGenes]
        }
        const genesEmpty = genes.filter(gene => {
            const {geneId, geneName, taxId, speciesName} = gene;
            return [geneId, geneName, taxId, speciesName].some(field => (
                !field || !String(field).length
            ));
        });
        return genesEmpty.length;
    }

    areGenesEmpty() {
        if (this.isParasite) {
            if (this.isAddMode) {
                if (!this.addedGenes || !this.addedGenes.length) return true;
            } else {
                if (this.targetModel.genesTotal <= this.pageSize) {
                    if (!this.targetModel.genes || !this.targetModel.genes.length) {
                        if (!this.addedGenes || !this.addedGenes.length) return true;
                    }
                }
            }
        } else {
            if (!this.targetModel.genes || !this.targetModel.genes.length) return true;
        }
    }

    needSaveGeneChanges () {
        return !!(this.parasiteGenesRemoved() || this.parasiteGenesAdded() || this.targetGenesChanged());
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
                    this.dispatcher.emit('target:model:changed');
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
            targetGeneId: g.geneId,
            geneId: g.geneId,
            geneName: g.geneName,
            taxId: g.taxId,
            speciesName: g.speciesName,
            priority: g.priority,
            ...g.metadata
        }));
        this.originalModel.targetGenes = (genes.items || []).map(g => ({
            targetGeneId: g.geneId,
            geneId: g.geneId,
            geneName: g.geneName,
            taxId: g.taxId,
            speciesName: g.speciesName,
            priority: g.priority,
            ...g.metadata
        }));
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

    addNewGene() {
        const model = this.isAddMode ? this.targetModel : this.originalModel;
        if (model.type === this.targetType.PARASITE) {
            this.addedGenes.push({...NEW_GENE});
            this.dispatcher.emit('target:form:gene:added');
        } else if (model.type === this.targetType.DEFAULT) {
            this._targetModel.genes.push({...NEW_GENE});
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

    postNewTarget() {
        const request = this.getAddRequest();
        return new Promise(resolve => {
            this.targetDataService.postNewTarget(request)
                .then(result => {
                    if (result) {
                        this.failed = false;
                        this.errorMessageList = null;
                        this.setTargetModel(result);
                        this.originalModel = result;
                        this.dispatcher.emit('target:model:changed');
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

    getAddParasiteRequest() {
        const {name, diseases, products, type} = this.targetModel;
        const request = {
            targetName: name,
            type,
            diseases,
            products,
        };
        return request;
    }

    getParasiteGenesRequest(genes, targetId) {
        const request = genes.map(g => {
            const gene = {
                targetId: targetId,
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

    async postNewParasiteTargetGenes(targetId) {
        const genesRequest = this.getParasiteGenesRequest(this.addedGenes, targetId);
        return new Promise((resolve, reject) => {
            this.targetDataService.postTargetGenes(targetId, genesRequest)
                .then(() => resolve(true))
                .catch(err => reject(err));
        });
    }

    async postNewParasiteTarget() {
        const targetRequest = this.getAddParasiteRequest();
        return new Promise(resolve => {
            this.targetDataService.postNewTarget(targetRequest)
                .then(async (target) => {
                    if (target) {
                        await this.postNewParasiteTargetGenes(target.targetId)
                            .then(success => {
                                if (success) {
                                    this.failed = false;
                                    this.errorMessageList = null;
                                    this.addedGenes = [];
                                    this.setTargetModel(target);
                                    this.originalModel = target;
                                    this.dispatcher.emit('target:model:changed');
                                }
                            });
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
                        this.dispatcher.emit('target:model:changed');
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

    getParasiteGenesMetadataRequest() {
        const {id, genes} = this.targetModel;
        const original = this.originalModel.targetGenes;
        const request = genes
            .filter(gene => {
                const {targetGeneId} = gene;
                const originalGene = original.filter(o => o.targetGeneId === targetGeneId)[0];
                return Object.entries(originalGene).some(([key, value]) => {
                    return gene[key] !== value;
                });
            })
            .map(g => {
                const metadata = this.metadataFields.map(c => [c, g[c]]);
                const gene = {
                    targetId: id,
                    geneId: g.geneId,
                    geneName: g.geneName,
                    taxId: g.taxId,
                    speciesName: g.speciesName,
                    metadata: Object.fromEntries(metadata)
                };
            if (g.priority && g.priority !== 'None') {
                gene.priority = g.priority;
            }
            return gene;
        });
        return request;
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
            promises.push(await this.postNewParasiteTargetGenes(this.targetModel.id));
        }
        if (this.parasiteGenesRemoved()) {
            const geneIds = this.removedGenes.map(gene => gene.geneId);
            promises.push(await this.deleteParasiteTargetGenes(this.targetModel.id, geneIds));
        }
        if (this.targetGenesChanged()) {
            const request = this.getParasiteGenesMetadataRequest();
            promises.push(await this.putParasiteGenes(request));
        }
        if (this.targetInfoChanged()) {
            const request = this.getParasiteTargetRequest();
            promises.push(await this.putParasiteTarget(request));
        }
        promises.push(this.getParasiteTarget());
        return await Promise.all(promises).then(values => {
            if (values.every(v => v)) {
                this.failed = false;
                this.errorMessageList = null;
                this.addedGenes = [];
                this.removedGenes = [];
            }
            this.loading = false;
            return true;
        })
            .catch(err => {
                this.failed = true;
                this.errorMessageList = [err.message];
                this.loading = false;
                return false;
            })
    }

    getParasiteTarget() {
        const id = this.targetModel.id;
        return new Promise((resolve, reject) => {
            this.targetDataService.getTargetById(id)
                .then(data => {
                    this.setTargetModel(data);
                    this.originalModel = data;
                    resolve(true);
                })
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

    putParasiteTarget(request) {
        return new Promise((resolve, reject) => {
            this.targetDataService.updateTarget(request)
                .then(target => {
                    if (target) {
                        this.setTargetModel(target);
                        this.originalModel = target;
                        resolve(true);
                    }
                })
                .catch(err => reject(err));
        });
    }

    deleteParasiteTargetGenes(targetId, geneIds) {
        return new Promise((resolve, reject) => {
            this.targetDataService.deleteTargetGenes(targetId, geneIds)
                .then(() => resolve(true))
                .catch(err => reject(err));
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
        this.removedGenes = [];
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
            } else {
                if (index !== -1 && geneFields[field]) {
                    this._targetModel.genes[index][geneFields[field]] = value;
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

    setMetadataFields(fields) {
        this.metadataFields = fields.filter(f => !this.defaultFields.includes(f));
    }

    setColumnsList() {
        const targetId = this.targetModel.id;
        return new Promise(resolve => {
            this.targetDataService.getTargetGenesFields(targetId)
                .then(columns => {
                    this.setMetadataFields(columns);
                    resolve(columns)
                })
                .catch(err => resolve([]));
        });
    }
}
