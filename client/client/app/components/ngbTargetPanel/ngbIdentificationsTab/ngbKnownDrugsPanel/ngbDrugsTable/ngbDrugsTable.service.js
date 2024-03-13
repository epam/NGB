const PAGE_SIZE = 10;

const OPEN_TARGETS_COLUMNS = ['target', 'drug', 'type', 'mechanism of action', 'action type', 'disease', 'phase', 'status', 'source'];
const PHARM_GKB_COLUMNS = ['target', 'drug', 'Source'];
const DGI_DB_COLUMNS = ['target', 'drug', 'interaction claim source', 'interaction types'];
const TTD_COLUMNS = ['target', 'ttd target', 'drug', 'company', 'type', 'therapeutic class', 'inChI', 'inChIKey', 'canonical smiles', 'status', 'compound class'];

const DISPLAY_NAME = {
    'ttd target': 'TTD target',
    'inChI': 'inChI',
    'inChIKey': 'inChIKey',
};

const FIELDS = {
    OPEN_TARGETS: {
        'target': 'GENE_ID',
        'drug': 'DRUG_NAME',
        'type': 'DRUG_TYPE',
        'mechanism of action': 'MECHANISM_OF_ACTION',
        'action type': 'ACTION_TYPE',
        'disease': 'DISEASE_NAME',
        'phase': 'PHASE',
        'status': 'STATUS',
        'source': 'SOURCE'
    },
    PHARM_GKB: {
        'target': 'GENE_ID',
        'drug': 'DRUG_NAME',
        'Source': 'SOURCE'
    },
    DGI_DB: {
        'target': 'GENE_ID',
        'drug': 'DRUG_NAME',
        'interaction claim source': 'INTERACTION_CLAIM_SOURCE',
        'interaction types': 'INTERACTION_TYPES'
    },
    TTD: {
        'target': 'TARGET',
        'ttd target': 'TTD_TARGET',
        'drug': 'DRUG_NAME',
        'company': 'COMPANY',
        'type': 'TYPE',
        'therapeutic class': 'THERAPEUTIC_CLASS',
        'inChI': 'INCHI',
        'inChIKey': 'INCHI_KEY',
        'canonical smiles': 'CANONICAL_SMILES',
        'status': 'STATUS',
        'compound class': 'COMPOUND_CLASS'
    }
};

const FILTER_FIELDS_LIST = {
    OPEN_TARGETS: {
        'drugTypes': 'type',
        'mechanismOfActions': 'mechanism of action',
        'actionTypes': 'action type',
        'phases': 'phase',
        'statuses': 'status',
        'sources': 'source'
    },
    PHARM_GKB: {
        'sources': 'Source'
    },
    DGI_DB: {
        'interactionClaimSources': 'interaction claim source',
        'interactionTypes': 'interaction types'
    },
    TTD: {
        'ttdTargets': 'ttd target',
        'companies': 'company',
        'types': 'type',
        'therapeuticClasses': 'therapeutic class',
        'statuses': 'status',
        'compoundClasses': 'compound class'
    }
};

export default class ngbDrugsTableService {

    _drugsResults = null;
    _totalPages = 0;
    _currentPage = 1;
    _sortInfo = null;
    _filterInfo = null;
    fieldList = {};
    _failedResult = false;
    _errorMessageList = null;
    _emptyResults = false;

    get pageSize() {
        return PAGE_SIZE;
    }
    get totalPages() {
        return this._totalPages;
    }
    get currentPage() {
        return this._currentPage;
    }
    set currentPage(value) {
        this._currentPage = value;
    }

    get loadingData() {
        return this.ngbKnownDrugsPanelService
            ? this.ngbKnownDrugsPanelService.loading
            : false;
    }
    set loadingData(value) {
        if (this.ngbKnownDrugsPanelService) {
            this.ngbKnownDrugsPanelService.loading = value;
        }
    }

    get failedResult() {
        return this._failedResult;
    }
    set failedResult(value) {
        this._failedResult = value;
    }

    get errorMessageList() {
        return this._errorMessageList;
    }
    set errorMessageList(value) {
        this._errorMessageList = value;
    }

    get emptyResults() {
        return this._emptyResults;
    }
    get drugsResults() {
        return this._drugsResults;
    }
    get sortInfo() {
        return this._sortInfo;
    }
    set sortInfo(value) {
        this._sortInfo = value;
    }
    get filterInfo() {
        return this._filterInfo;
    }
    set filterInfo(value) {
        this._filterInfo = value;
    }

    get fields () {
        return FIELDS;
    }
    get filterFieldsList () {
        return FILTER_FIELDS_LIST
    }

    get openTargetsColumns() {
        return OPEN_TARGETS_COLUMNS;
    }
    get pharmGkbColumns() {
        return PHARM_GKB_COLUMNS;
    }
    get dgiDbColumns() {
        return DGI_DB_COLUMNS;
    }
    get ttdColumns() {
        return TTD_COLUMNS;
    }
    get displayName() {
        return DISPLAY_NAME;
    }

    static instance (dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService) {
        return new ngbDrugsTableService(dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService});
        this.dispatcher.on('target:identification:reset', this.resetDrugsData.bind(this));
    }

    get sourceModel () {
        return this.ngbKnownDrugsPanelService.sourceModel;
    }

    get sourceOptions() {
        return this.ngbKnownDrugsPanelService.sourceOptions;
    }

    getColumnName(field) {
        if (Object.prototype.hasOwnProperty.call(this.displayName, field)) {
            return this.displayName[field];
        }
        return field.charAt(0).toUpperCase() + field.slice(1);
    }

    getColumnList() {
        const {OPEN_TARGETS, PHARM_GKB, DGI_DB, TTD} = this.sourceOptions;
        if (this.sourceModel === OPEN_TARGETS) {
            return this.openTargetsColumns;
        }
        if (this.sourceModel === PHARM_GKB) {
            return this.pharmGkbColumns;
        }
        if (this.sourceModel === DGI_DB) {
            return this.dgiDbColumns;
        }
        if (this.sourceModel === TTD) {
            return this.ttdColumns;
        }
    }

    get geneIds() {
        return [...this.ngbTargetPanelService.allGenes.map(i => i.geneId)];
    }

    getTarget(id) {
        if (!id) return;
        return this.ngbTargetPanelService.getChipByGeneId(id);
    }

    setDrugsResult(result) {
        const {OPEN_TARGETS, PHARM_GKB, DGI_DB, TTD} = this.sourceOptions;
        if (this.sourceModel === OPEN_TARGETS) {
            this._drugsResults = result.map(item => ({
                target: {
                    geneId: item.geneId,
                    value: this.getTarget(item.geneId),
                },
                drug: {
                    id: item.id,
                    name: item.name,
                    url: item.url
                },
                type: item.drugType,
                'mechanism of action': item.mechanismOfAction,
                'action type': item.actionType,
                disease: item.disease,
                phase: item.phase,
                status: item.status,
                source: item.source
            }));
        }
        if (this.sourceModel === PHARM_GKB) {
            this._drugsResults = result.map(item => ({
                target: {
                    geneId: item.geneId,
                    value: this.getTarget(item.geneId)
                },
                'drug': {
                    id: item.id,
                    name: item.name,
                    url: item.url
                },
                'Source': item.source
            }));
        }
        if (this.sourceModel === DGI_DB) {
            this._drugsResults = result.map(item => ({
                target: {
                    geneId: item.geneId,
                    value: this.getTarget(item.geneId)
                },
                'drug': {
                    id: item.id,
                    name: item.name,
                    url: item.url
                },
                'interaction claim source': item.interactionClaimSource,
                'interaction types': item.interactionTypes
            }));
        }
        if (this.sourceModel === TTD) {
            this._drugsResults = result.map(item => {
                return {
                    target: {
                        geneId: item.geneId,
                        value: this.getTarget(item.geneId)
                    },
                    'ttd target': item.ttdTarget,
                    'drug': {
                        id: item.id,
                        name: item.name,
                        url: item.url
                    },
                    'company': item.company,
                    'type': item.type,
                    'therapeutic class': item.therapeuticClass,
                    'inChI': item.inChI,
                    'inChIKey': item.inChIKey,
                    'canonical smiles': item.canonicalSmiles,
                    'status': item.status,
                    'compound class': item.compoundClass,
                };
            });
        }
    }

    setFilter(field, value) {
        const filter = {...(this._filterInfo || {})};
        if (value && value.length) {
            filter[field] = value;
        } else {
            delete filter[field];
        }
        this._filterInfo = filter;
    }

    getRequest() {
        const request = {
            page: this.currentPage,
            pageSize: this.pageSize,
            geneIds: this.geneIds,
        };
        if (this.sourceModel.name === this.sourceOptions.TTD.name) {
            if (this._filterInfo && this._filterInfo.target && this._filterInfo.target.length) {
                const geneIds = this._filterInfo.target
                    .map(t => {
                        const chip = this.ngbTargetPanelService.getGeneIdByChip(t);
                        return chip ? chip : '';
                    })
                    .filter(v => v);
                if (geneIds && geneIds.length) {
                    request.geneIds = geneIds;
                }
            }
        }
        if (this.sortInfo && this.sortInfo.length) {
            request.orderInfos = this.sortInfo.map(i => ({
                orderBy: this.fields[this.sourceModel.name][i.field],
                reverse: !i.ascending
            }))
        }
        if (this._filterInfo) {
            const filters = Object.entries(this._filterInfo)
                .filter(([key, values]) => {
                    if (this.sourceModel.name === this.sourceOptions.TTD.name) {
                        if (key === 'target') {
                            return;
                        }
                    }
                    if (!this.fields[this.sourceModel.name][key]) return;
                    return values.length
                })
                .map(([key, values]) => ({
                    field: this.fields[this.sourceModel.name][key],
                    terms: values.map(v => {
                        if (key === 'phase') {
                            return v || '';
                        }
                        if (key === 'target') {
                            const chip = this.ngbTargetPanelService.getGeneIdByChip(v);
                            return chip ? chip : '';
                        }
                        if (v === 'Empty value') {
                            return '';
                        }
                        return v;
                    })
                }));
            if (filters && filters.length) {
                request.filters = filters;
            }
        }
        return request;
    }

    getDrugsResults() {
        const request = this.getRequest();
        if (!request.geneIds || !request.geneIds.length) {
            return new Promise(resolve => {
                this.loadingData = false;
                resolve(true);
            });
        }
        const source = this.sourceModel.name;
        return new Promise(resolve => {
            this.targetDataService.getDrugsResults(request, source)
                .then(([data, totalCount]) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyResults = totalCount === 0;
                    this.setDrugsResult(data);
                    this.loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._totalPages = 0;
                    this._emptyResults = false;
                    this.loadingData = false;
                    resolve(false);
                });
        });
    }

    async setFieldList() {
        const result = await this.getDrugsFieldValues();
        if (!result) {
            this.fieldList = {};
            this.dispatcher.emitSimpleEvent('target:identification:drugs:filters:list');
        }
        const entries = Object.entries(result);
        const source = this.sourceModel.name;
        const list = this.filterFieldsList;
        for (let i = 0; i < entries.length; i++) {
            const key = entries[i][0];
            const values = (entries[i][1] || []).map(v => v ? v : 'Empty value');
            const field = list[source][key]
            this.fieldList[field] = values;
        }
        this.fieldList.target = [...this.ngbTargetPanelService.allChips];
        this.dispatcher.emitSimpleEvent('target:identification:drugs:filters:list');
    }

    getDrugsFieldValues() {
        const geneIds = this.geneIds;
        if (!geneIds || !geneIds.length) {
            return new Promise(resolve => {
                resolve(null);
            });
        }
        const source = this.sourceModel.name;
        return new Promise(resolve => {
            this.targetDataService.getDrugsFieldValues(source, geneIds)
                .then((data) => {
                    resolve(data);
                })
                .catch(err => {
                    resolve(null);
                });
        });
    }

    resetDrugsData() {
        this._drugsResults = null;
        this._currentPage = 1;
        this._totalPages = 0;
        this._sortInfo = null;
        this._filterInfo = null;
        this.fieldList = {};
        this.loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
    }
}
