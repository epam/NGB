const PAGE_SIZE = 10;

function romanize (num) {
    if (isNaN(num)) {
        return NaN;
    }
    const lookup = [
        ['M', '1000'],
        ['CM', '900'],
        ['D',  '500'],
        ['CD', '400'],
        ['C',  '100'],
        ['XC',  '90'],
        ['L',   '50'],
        ['XL',  '40'],
        ['X',   '10'],
        ['IX',   '9'],
        ['V',    '5'],
        ['IV',   '4'],
        ['I',    '1'],
    ];
    let roman = '';
    for (let i = 0; i < lookup.length; i++) {
        const [letter, number] = lookup[i];
        while (num >= Number(number)) {
            roman += letter;
            num -= Number(number);
        }
    }
    return roman;
}

const SORT_FIELDS = {
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
        'drug name': 'DRUG_NAME',
        'Source': 'SOURCE'
    },
    DGI_DB: {
        'target': 'GENE_ID',
        'drug name': 'DRUG_NAME',
        'interaction claim source': 'INTERACTION_CLAIM_SOURCE',
        'interaction types': 'INTERACTION_TYPES'
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

    get sortFields () {
        return SORT_FIELDS;
    }

    static instance (dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService) {
        return new ngbDrugsTableService(dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService});
        this.dispatcher.on('reset:identification:data', this.resetDrugsData.bind(this));
    }

    get identificationTarget() {
        return this.ngbTargetPanelService.identificationTarget || {};
    }

    get sourceModel () {
        return this.ngbKnownDrugsPanelService.sourceModel;
    }

    get geneIds() {
        const {interest, translational} = this.identificationTarget;
        if (!this._filterInfo || !this._filterInfo.target) {
            return [...interest.map(i => i.geneId), ...translational.map(t => t.geneId)];
        }
        if (this._filterInfo.target) {
            return [...interest
                    .filter(i => this._filterInfo.target.includes(i.chip))
                    .map(i => i.geneId),
                ...translational
                    .filter(i => this._filterInfo.target.includes(i.chip))
                    .map(t => t.geneId)];
        }
    }

    getTarget(id) {
        if (!id) return;
        return this.ngbTargetPanelService.getChipByGeneId(id);
    }

    setDrugsResult(result) {
        const sourceOptions = this.ngbKnownDrugsPanelService.sourceOptions;

        if (this.sourceModel === sourceOptions.OPEN_TARGETS) {
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
                phase: `Phase ${romanize(item.phase)}`,
                status: item.status,
                source: item.source
            }));
        }
        if (this.sourceModel === sourceOptions.PHARM_GKB) {
            this._drugsResults = result.map(item => ({
                target: {
                    geneId: item.geneId,
                    value: this.getTarget(item.geneId)
                },
                'drug name': item.drugName,
                'Source': item.source
            }));
        }
        if (this.sourceModel === sourceOptions.DGI_DB) {
            this._drugsResults = result.map(item => ({
                target: {
                    geneId: item.geneId,
                    value: this.getTarget(item.geneId)
                },
                'drug name': item.drugName,
                'interaction claim source': item.interactionClaimSource,
                'interaction types': item.interactionTypes
            }));
        }
    }

    setFieldList() {
        const {interest, translational} = this.identificationTarget;
        this.fieldList = {
            target: [...interest.map(i => i.chip), ...translational.map(t => t.chip)],
            disease: []
        };
        this.dispatcher.emitSimpleEvent('drugs:filters:list');
    }

    setFilter(field, value) {
        const filter = {...(this._filterInfo || {})};
        filter[field] = value;
        this._filterInfo = filter;
    }

    getRequest() {
        const request = {
            page: this.currentPage,
            pageSize: this.pageSize,
            geneIds: this.geneIds,
        };
        if (this.sortInfo && this.sortInfo.length) {
            const {field, ascending} = this.sortInfo[0];
            request.reverse = !ascending;
            request.orderBy = this.sortFields[this.sourceModel.name][field];
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

    resetDrugsData() {
        this._drugsResults = null;
        this._currentPage = 1;
        this._totalPages = 0;
        this._sortInfo = null;
        this._filterInfo = null;
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
    }
}
