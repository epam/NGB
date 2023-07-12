const PAGE_SIZE = 10;

const OPEN_TARGETS_COLUMNS = ['target', 'drug', 'type', 'mechanism of action', 'action type', 'disease', 'phase', 'status', 'source'];
const PHARM_GKB_COLUMNS = ['target', 'drug', 'Source'];
const DGI_DB_COLUMNS = ['target', 'drug', 'interaction claim source', 'interaction types'];

const ROMAN = {
    M: '1000',
    CM: '900',
    D:  '500',
    CD: '400',
    C:  '100',
    XC:  '90',
    L:   '50',
    XL:  '40',
    X:   '10',
    IX:   '9',
    V:    '5',
    IV:   '4',
    I:    '1',
};

function romanize (num) {
    if (isNaN(num)) return;
    if (num === '0') return 'Phase I (Early)';
    const lookup = Object.entries(ROMAN);
    let roman = '';
    for (let i = 0; i < lookup.length; i++) {
        const [letter, number] = lookup[i];
        while (num >= Number(number)) {
            roman += letter;
            num -= Number(number);
        }
    }
    return roman ? `Phase ${roman}` : '';
}

function unRomanize(phase) {
    if (!phase) return;
    if (phase === 'Phase I (Early)') return 0;
    const roman = phase.replace('Phase ', '').toUpperCase();
    if (ROMAN[roman]) return Number(ROMAN[roman]);
    let num = 0;
    const arr = roman.split('');
    while (arr.length) {
        num += +ROMAN[arr.shift()];
    }
    return num;
}

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
    }
};

export default class ngbDrugsTableService {

    _drugsResults = null;
    _totalPages = 0;
    _currentPage = 1;
    _sortInfo = null;
    _filterInfo = null;
    fieldList = {};

    _loadingData = false;
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
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
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

    static instance (dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService) {
        return new ngbDrugsTableService(dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService});
        this.dispatcher.on('reset:identification:data', this.resetDrugsData.bind(this));
    }

    get sourceModel () {
        return this.ngbKnownDrugsPanelService.sourceModel;
    }

    get sourceOptions() {
        return this.ngbKnownDrugsPanelService.sourceOptions;
    }

    getColumnList() {
        const {OPEN_TARGETS, PHARM_GKB, DGI_DB} = this.sourceOptions;
        if (this.sourceModel === OPEN_TARGETS) {
            return this.openTargetsColumns;
        }
        if (this.sourceModel === PHARM_GKB) {
            return this.pharmGkbColumns;
        }
        if (this.sourceModel === DGI_DB) {
            return this.dgiDbColumns;
        }
    }

    get geneIds() {
        const allGenes = this.ngbTargetPanelService.allGenes;
        if (this._filterInfo && this._filterInfo.target) {
            return [...allGenes
                    .filter(i => this._filterInfo.target.includes(i.chip))
                    .map(i => i.geneId)];
        }
        return [...allGenes.map(i => i.geneId)];
    }

    getTarget(id) {
        if (!id) return;
        return this.ngbTargetPanelService.getChipByGeneId(id);
    }

    setDrugsResult(result) {
        const {OPEN_TARGETS, PHARM_GKB, DGI_DB} = this.sourceOptions;
        if (this.sourceModel === OPEN_TARGETS) {
            this._drugsResults = result.map(item => ({
                target: {
                    geneId: item.geneId,
                    value: this.getTarget(item.geneId),
                },
                drug: item.drug,
                type: item.drugType,
                'mechanism of action': item.mechanismOfAction,
                'action type': item.actionType,
                disease: item.disease,
                phase: romanize(item.phase),
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
                    name: item.name,
                    url: item.url
                },
                'Source': item.source
            }));
        }
        if (this.sourceModel === DGI_DB) {
            this._drugsResults = result.map(item => ({
                target: {
                    geneId: item.entrezId,
                    value: this.getTarget(item.entrezId)
                },
                'drug': {
                    name: item.name,
                    url: item.url
                },
                'interaction claim source': item.interactionClaimSource,
                'interaction types': item.interactionTypes
            }));
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
        if (this.sortInfo && this.sortInfo.length) {
            request.sortInfos = this.sortInfo.map(i => i)
        }
        if (this._filterInfo) {
            const filters = Object.entries(this._filterInfo)
                .filter(([key, values]) => values.length)
                .map(([key, values]) => {
                    console.log(key, values)
                    return {
                        field: this.fields[this.sourceModel.name][key],
                        terms: values.map(v => {
                            if (key === 'phase') {
                                return unRomanize(v);
                            }
                            if (key === 'target') {
                                return this.ngbTargetPanelService.getGeneIdByChip(v);
                            }
                            return v;
                        })
                    };
                });
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
                this._loadingData = false;
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
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._totalPages = 0;
                    this._emptyResults = false;
                    this._loadingData = false;
                    resolve(false);
                });
        });
    }

    async setFieldList() {
        const result = await this.getDrugsFieldValues();
        if (!result) {
            this.fieldList = {};
            this.dispatcher.emitSimpleEvent('drugs:filters:list');
        }
        const entries = Object.entries(result);
        const source = this.sourceModel.name;
        const list = this.filterFieldsList;
        for (let i = 0; i < entries.length; i++) {
            const key = entries[i][0];
            const values = entries[i][1].filter(v => v);
            const field = list[source][key]
            if (field === 'phase') {
                this.fieldList[field] = values.map(v => romanize(v));
            } else {
                this.fieldList[field] = values;
            }
        }
        const allGenes = this.ngbTargetPanelService.allGenes;
        this.fieldList.target = [...allGenes.map(i => i.chip)]
        this.dispatcher.emitSimpleEvent('drugs:filters:list');
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
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
    }
}
