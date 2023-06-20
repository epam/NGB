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

export default class ngbDrugsTableService {

    _drugsResults = null;
    _totalPages = 0;
    _currentPage = 1;
    _sortInfo = null;

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

    static instance (dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService,) {
        return new ngbDrugsTableService(dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbKnownDrugsPanelService, ngbTargetPanelService, targetDataService});
        this.dispatcher.on('reset:identification:data', this.resetDrugsData.bind(this));
    }

    get geneIds() {
        const {interest, translational} = this.ngbTargetPanelService.identificationTarget || {};
        return [...interest.map(i => i.geneId), ...translational.map(t => t.geneId)];
    }

    setDrugsResult(result) {
        const source = this.ngbKnownDrugsPanelService.sourceModel;
        const sourceOptions = this.ngbKnownDrugsPanelService.sourceOptions;

        if (source === sourceOptions.OPEN_TARGETS) {
            this._drugsResults = result.map(item => ({
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
        if (source === sourceOptions.PHARM_GKB) {
            this._drugsResults = result.map(item => ({
                'drug id': item.drugId,
                'drug name': item.drugName,
                'gene id': item.geneId,
                'Source': item.source
            }));
        }
        if (source === sourceOptions.DGI_DB) {
            'drug name', 'entrez id', 'gene name', ''
            this._drugsResults = result.map(item => ({
                'drug name': item.drugName,
                'entrez id': item.entrezId,
                'gene name': item.geneName,
                'interaction claim source': item.interactionClaimSource
            }));
        }
        if (source === sourceOptions.TXGNN) {
            this._drugsResults = [];
        }
    }

    getRequest() {
        return {
            page: this.currentPage,
            pageSize: this.pageSize,
            geneIds: this.geneIds,
        };
    }

    getDrugsResults() {
        const request = this.getRequest();
        const source = this.ngbKnownDrugsPanelService.sourceModel.name;
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
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
    }
}
