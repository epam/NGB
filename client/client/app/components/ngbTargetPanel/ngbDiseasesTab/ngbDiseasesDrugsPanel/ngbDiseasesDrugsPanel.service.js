const PAGE_SIZE = 10;

const FIELDS = {
    'target': 'GENE_SYMBOL',
    'drug': 'DRUG_NAME',
    'type': 'DRUG_TYPE',
    'mechanism of action': 'MECHANISM_OF_ACTION',
    'action type': 'ACTION_TYPE',
    'target name': 'GENE_NAME',
    'phase': 'PHASE',
    'status': 'STATUS',
    'source': 'SOURCE'
};

const FILTER_FIELDS = {
    'drugTypes': 'type',
    'mechanismOfActions': 'mechanism of action',
    'actionTypes': 'action type',
    'phases': 'phase',
    'statuses': 'status',
    'sources': 'source'
};

export default class ngbDiseasesDrugsPanelService {

    get pageSize() {
        return PAGE_SIZE;
    }

    get fields() {
        return FIELDS;
    }

    get filterFields() {
        return FILTER_FIELDS;
    }

    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _emptyResults = false;
    _totalPages = 0;
    _currentPage = 1;
    _sortInfo = null;
    _filterInfo = null;
    fieldList = {};
    _drugsResults = null;

    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
    }
    get failedResult() {
        return this._failedResult;
    }
    get errorMessageList() {
        return this._errorMessageList;
    }
    get emptyResults() {
        return this._emptyResults;
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
    get drugsResults() {
        return this._drugsResults;
    }

    static instance (dispatcher, ngbDiseasesTabService, targetDataService) {
        return new ngbDiseasesDrugsPanelService(dispatcher, ngbDiseasesTabService, targetDataService);
    }

    constructor(dispatcher, ngbDiseasesTabService, targetDataService) {
        Object.assign(this, {dispatcher, ngbDiseasesTabService, targetDataService});
        dispatcher.on('target:diseases:disease:changed', this.resetData.bind(this));
    }

    get diseaseId() {
        return (this.ngbDiseasesTabService.diseasesData || {}).id;
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

    setDrugsResult(result) {
        this._drugsResults = result.map(item => ({
            target: item.geneSymbol,
            drug: {
                id: item.id,
                name: item.name,
                url: item.url,
            },
            type: item.drugType,
            'mechanism of action': item.mechanismOfAction,
            'action type': item.actionType,
            'target name': item.geneName,
            phase: item.phase,
            status: item.status,
            source: item.source
        }));
    }

    getRequest() {
        const request = {
            page: this.currentPage,
            pageSize: this.pageSize,
        };
        if (this.sortInfo && this.sortInfo.length) {
            request.orderInfos = this.sortInfo.map(i => ({
                orderBy: this.fields[i.field],
                reverse: !i.ascending
            }));
        }
        if (this._filterInfo) {
            const filters = Object.entries(this._filterInfo)
                .filter(([key, values]) => values.length)
                .map(([key, values]) => {
                    return {
                        field: this.fields[key],
                        terms: values.map(v => {
                            if (key === 'phase') {
                                return v || '';
                            }
                            if (v === 'Empty value') {
                                return '';
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
        if (!this.diseaseId) {
            return new Promise(resolve => {
                this.loadingData = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getDiseasesDrugsResults(this.diseaseId, request)
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
            this.dispatcher.emit('target:diseases:drugs:filters:list');
            return;
        }
        const entries = Object.entries(result);
        const list = this.filterFields;
        for (let i = 0; i < entries.length; i++) {
            const key = entries[i][0];
            const values = entries[i][1].map(v => !v ? 'Empty value' : v);
            const field = list[key]
            this.fieldList[field] = values;
        }
        this.dispatcher.emit('target:diseases:drugs:filters:list');
    }

    getDrugsFieldValues() {
        if (!this.diseaseId) {
            return new Promise(resolve => {
                resolve(null);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getDiseasesDrugsFieldValues(this.diseaseId)
                .then((data) => {
                    resolve(data);
                })
                .catch(err => {
                    resolve(null);
                });
        });
    }

    resetData() {
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
        this._totalPages = 0;
        this._currentPage = 1;
        this._sortInfo = null;
        this._filterInfo = null;
        this.fieldList = {};
        this._drugsResults = null;
    }

    exportResults() {
        if (!this.diseaseId) {
            return new Promise(resolve => {
                resolve(true);
            });
        }
        const source = 'drugs';
        return this.targetDataService.getDiseasesExport(this.diseaseId, source);
    }
}
