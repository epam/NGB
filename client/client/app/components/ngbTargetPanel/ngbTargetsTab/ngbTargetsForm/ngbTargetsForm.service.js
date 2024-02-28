const NEW_ADDITIONAL_GENE = {
    taxId: '',
    geneId: '',
};

const NEW_ADDITIONAL_GENES = {
    limit: 1,
}

const NEW_GENE = {
    geneId: '',
    geneName: '',
    taxId: '',
    speciesName: '',
    priority: '',
};

const getNewGene = () => {
    const gene = {...NEW_GENE};
    gene.additionalGenes = {...NEW_ADDITIONAL_GENES};
    gene.additionalGenes.value = [{...NEW_ADDITIONAL_GENE}];
    return gene;
}

const PAGE_SIZE = 20;
const ADDITIONAL_GENES = 'additionalGenes';

const GENE_MODEL_PROPERTIES = ['geneId', 'geneName', 'taxId', 'speciesName', 'priority', 'additionalGenes'];
const DEFAULT_FIELDS = ['Gene ID', 'Gene Name', 'Tax ID', 'Species Name', 'Priority', 'Additional Genes'];

const REQUIRED_FIELDS = ['geneId', 'geneName', 'taxId', 'speciesName'];

export const encodeName = (name) => {
    if (name.includes('(') || name.includes(')')) {
        return name.replace('(', '_(').replace(')', ')_');
    }
    return name;
};

export const decodeName = (name) => {
    if (name.includes('_(') || name.includes(')_')) {
        return name.replace('_(', '(').replace(')_', ')');
    }
    return name;
};

const encodedMetadata = (data) => {
    if (!data) return {};
    return Object.fromEntries(Object.entries(data)
        .map(([name, value]) => [encodeName(name), value]))
};

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
    get requiredFields() {
        return REQUIRED_FIELDS;
    }
    get additionalGenes() {
        return ADDITIONAL_GENES;
    }

    _targetModel;
    _originalModel;
    _updateForce = false;
    _addedGenes = [];
    _removedGenes = [];
    metadataFields = [];
    _geneFile = null;

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
    get geneFile() {
        return this._geneFile;
    }
    set geneFile(value) {
        this._geneFile = value;
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
        const originalGenes = this.originalModel.genes;
        const targetGenes = this.targetModel.genes;
        if (originalGenes.length !== targetGenes.length) return true;
        const allFields = [...this.geneModelProperties, ...this.metadataFields];
        return targetGenes.some((gene, index) => (
            Object.entries(gene)
                .some(([key, value]) => {
                    if (!allFields.includes(key)) return false;
                    if (key === this.additionalGenes) {
                        const additionalGenes = value.value.filter(v => v.geneId && v.taxId);
                        const originalAdditionalGenes = originalGenes[index].additionalGenes.value;
                        if (originalAdditionalGenes.length !== additionalGenes.length) return true;
                        return additionalGenes.some((g, i) => (
                            g.geneId !== originalAdditionalGenes[i].geneId ||
                            g.taxId !== originalAdditionalGenes[i].taxId
                        ))
                    } else {
                        return String(value) !== String(originalGenes[index][key]);
                    }
                })
        ));
    }

    targetInfoChanged() {
        const nameChanged = this.originalModel.name !== this.targetModel.name;
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
            genes = [...genes, ...this.addedGenes];
        }
        const genesEmpty = genes.filter(gene => {
            return this.requiredFields.some(field => (
                !gene[field] || !String(field).length
            ))
        });
        return genesEmpty.length;
    }

    isSomeAdditionalGeneEmpty() {
        let {genes} = this.targetModel;
        if (this.isParasite) {
            genes = [...genes, ...this.addedGenes];
        }
        const genesEmpty = genes.filter(gene => {
            if (!gene.additionalGenes || !gene.additionalGenes.value) return false;
            const additionalGenes = gene.additionalGenes.value;
            return additionalGenes.some(g => (!g.taxId || !String(g.taxId).length) ^
                                             (!g.geneId || !String(g.geneId).length))
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
            id: data.id,
            name: data.targetName,
            genes: (data.targetGenes || []).map(gene => {
                const geneObject = {
                    geneId: gene.geneId,
                    geneName: gene.geneName,
                    taxId: gene.taxId,
                    speciesName: gene.speciesName,
                    priority: gene.priority,
                };
                if (gene.additionalGenes) {
                    geneObject.additionalGenes = {
                        value: Object.entries(gene.additionalGenes)
                            .map(([geneId, taxId]) => ({
                                geneId,
                                taxId,
                            })),
                        limit: 1,
                    };
                }
                return geneObject;
            }),
            diseases: (data.diseases || []).filter(d => d),
            products: (data.products || []).filter(p => p),
            identifications: data.identifications,
            type: data.type,
        };
        this.setOriginalModel(data);
        this.targetContext.targetModelType = data.type;
    }

    setOriginalModel(data) {
        this._originalModel = {
            id: data.id,
            name: data.targetName,
            diseases: (data.diseases || []).filter(d => d),
            products: (data.products || []).filter(p => p),
            identifications: data.identifications,
            type: data.type,
            genes: (data.targetGenes || []).map(gene => {
                const geneObject = {
                    geneId: gene.geneId,
                    geneName: gene.geneName,
                    taxId: gene.taxId,
                    speciesName: gene.speciesName,
                    priority: gene.priority,
                };
                if (gene.additionalGenes) {
                    geneObject.additionalGenes = {
                        value: Object.entries(gene.additionalGenes)
                            .map(([geneId, taxId]) => ({
                                geneId,
                                taxId,
                            })),
                        limit: 1,
                    };
                }
                return geneObject;
            })
        };
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
        this._targetModel.genes = (genes.items || []).map(g => {
            const gene = {
                targetGeneId: g.targetGeneId,
                geneId: g.geneId,
                geneName: g.geneName,
                taxId: g.taxId,
                speciesName: g.speciesName,
                priority: g.priority,
                ...encodedMetadata(g.metadata)
            };
            if (g.additionalGenes) {
                gene.additionalGenes = {
                    value: Object.entries(g.additionalGenes || {})
                        .map(([geneId, taxId]) => ({
                            geneId,
                            taxId,
                        })),
                    limit: 1,
                }
            }
            return gene;
        });
        this._originalModel.genes = (genes.items || []).map(g => {
            const gene = {
                targetGeneId: g.targetGeneId,
                geneId: g.geneId,
                geneName: g.geneName,
                taxId: g.taxId,
                speciesName: g.speciesName,
                priority: g.priority,
                ...encodedMetadata(g.metadata)
            };
            if (g.additionalGenes) {
                gene.additionalGenes = {
                    value: Object.entries(g.additionalGenes)
                        .map(([geneId, taxId]) => ({
                            geneId,
                            taxId,
                        })),
                    limit: 1,
                };
            }
            return gene;
        });
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
            const newGene = getNewGene();
            this.addedGenes.push(newGene);
            this.dispatcher.emit('target:form:gene:added');
        } else if (model.type === this.targetType.DEFAULT) {
            const newGene = getNewGene();
            this._targetModel.genes.push(newGene);
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
            const gene = Object.fromEntries(this.requiredFields.map(field => (
                [field, g[field]]
            )));
            gene.targetId = targetId;
            gene.metadata = {};
            if (g.priority && g.priority !== 'None') {
                gene.priority = g.priority;
            }
            if (g.additionalGenes) {
                gene.additionalGenes = g.additionalGenes.value
                    .reduce((a, v) => ({ ...a, [v.geneId]: Number(v.taxId)}), {}) 
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

    importFile(targetId) {
        const formData = new FormData();
        formData.append('file', this.geneFile);
        return new Promise((resolve, reject) => {
            this.targetDataService.importGenes(targetId, formData)
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
                        const promises = [];
                        if (this.geneFile) {
                            promises.push(await this.importFile(target.id));
                        }
                        if (this.parasiteGenesAdded()) {
                            promises.push(await this.postNewParasiteTargetGenes(target.id));
                        }
                        await Promise.all(promises).then(values => {
                            if (values.every(v => v)) {
                                this.failed = false;
                                this.errorMessageList = null;
                                this.addedGenes = [];
                                this.geneFile = null;
                                this.setTargetModel(target);
                                this.dispatcher.emit('target:model:changed');
                            }
                        })
                            .catch(err => {
                                this.failed = true;
                                this.errorMessageList = [err.message];
                            })
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
            id,
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
        const original = this.originalModel.genes;
        const request = genes
            .filter(gene => {
                const allFields = [...this.geneModelProperties, ...this.metadataFields];
                const originalGene = original.filter(o => o.targetGeneId === gene.targetGeneId)[0];
                return Object.entries(gene).some(([key, value]) => {
                    if (!allFields.includes(key)) return false;
                    if (key === this.additionalGenes) {
                        const additionalGenes = value.value;
                        const originalAdditionalGenes = originalGene.additionalGenes.value;
                        if (originalAdditionalGenes.length !== additionalGenes.length) return true;
                        return additionalGenes.some((g, i) => (
                            g.geneId !== originalAdditionalGenes[i].geneId ||
                            g.taxId !== originalAdditionalGenes[i].taxId
                        ));
                    } else {
                        return String(value) !== String(originalGene[key]);
                    }
                });
            })
            .map(g => {
                const gene = Object.fromEntries(this.requiredFields.map(field => (
                    [field, g[field]]
                )));
                gene.targetGeneId = g.targetGeneId;
                gene.targetId = id;
                gene.metadata = Object.fromEntries(this.metadataFields.map(c => [c, g[c]]));
                if (g.priority && g.priority !== 'None') {
                    gene.priority = g.priority;
                }
                if (g.additionalGenes) {
                    gene.additionalGenes = g.additionalGenes.value.reduce((a, v) => ({ ...a, [v.geneId]: Number(v.taxId)}), {}) 
                }
                return gene;
            });
        return request;
    }

    getChangedFields() {
        const original = this.originalModel.genes;
        const allFields = [...this.geneModelProperties, ...this.metadataFields];
        const keys = this.targetModel.genes
            .reduce((fields, gene) => {
                const originalGene = original.filter(o => o.targetGeneId === gene.targetGeneId)[0];
                const entries = Object.entries(gene);
                for (let i = 0; i < entries.length; i++) {
                    const [key, value] = entries[i];
                    if (allFields.includes(key)) {
                        if (String(value) !== String(originalGene[key])) {
                            fields.push(key);
                        }
                    }
                }
                return fields;
            }, []);
        return new Set(keys)
    }

    getParasiteTargetRequest() {
        const {id, name, diseases, products, type} = this.targetModel;
        const request = {
            id,
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
        if (this.geneFile) {
            promises.push(this.importFile(this.targetModel.id));
        }
        if (this.parasiteGenesAdded()) {
            promises.push(this.postNewParasiteTargetGenes(this.targetModel.id));
        }
        if (this.parasiteGenesRemoved()) {
            const targetGeneIds = this.removedGenes.map(gene => gene.targetGeneId);
            promises.push(this.deleteParasiteTargetGenes(targetGeneIds));
        }
        if (this.targetGenesChanged()) {
            const request = this.getParasiteGenesMetadataRequest();
            if (request && request.length) {
                promises.push(this.putParasiteGenes(request));
            }
        }
        if (this.targetInfoChanged()) {
            const request = this.getParasiteTargetRequest();
            promises.push(this.putParasiteTarget(request));
        }
        promises.push(this.getParasiteTarget());
        return await Promise.all(promises).then(values => {
            if (values.every(v => v)) {
                this.failed = false;
                this.errorMessageList = null;
                this.addedGenes = [];
                this.removedGenes = [];
                this.geneFile = null;
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
        this._geneFile = null;
        this.metadataFields = [];
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
        this.metadataFields = fields.map(f => f.fieldName)
            .filter(name => !this.defaultFields.includes(name));
    }

    setTargetGenesFields() {
        return new Promise(resolve => {
            this.targetDataService.getTargetGenesFields(this.targetModel.id)
                .then(columns => {
                    this.setMetadataFields(columns);
                    resolve(columns);
                })
                .catch(err => {
                    this.setMetadataFields([]);
                    resolve([]);
                })
        })
    }
}
