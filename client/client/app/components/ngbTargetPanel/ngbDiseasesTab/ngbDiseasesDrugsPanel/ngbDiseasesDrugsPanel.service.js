const PAGE_SIZE = 10;

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
    'disease': 'DISEASE_NAME',
    'drug': 'DRUG_NAME',
    'type': 'DRUG_TYPE',
    'mechanism of action': 'MECHANISM_OF_ACTION',
    'action type': 'ACTION_TYPE',
    'target': 'TARGET',
    'phase': 'PHASE',
    'status': 'STATUS',
    'source': 'SOURCE'
};

export default class ngbDiseasesDrugsPanelService {

    get pageSize() {
        return PAGE_SIZE;
    }

    get fields() {
        return FIELDS;
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
        dispatcher.on('target:diseases:details:finished', this.resetData.bind(this));
        dispatcher.on('target:diseases:updated', this.updateData.bind(this));
    }

    get diseaseId() {
        return this.ngbDiseasesTabService.diseasesData.id;
    }

    setDrugsResult(result) {
        this._drugsResults = result.map(item => ({
            disease: item.disease,
            drug: item.drug,
            type: item.drugType,
            'mechanism of action': item.mechanismOfAction,
            'action type': item.actionType,
            target: item.target,
            phase: romanize(item.phase),
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

    updateData() {
        this.resetData();
        this.dispatcher.emit('target:diseases:drugs:updated');
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
}
