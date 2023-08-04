const SOURCE_OPTIONS = {
    PROTEIN_DATA_BANK: 'Protein Data Bank',
    LOCAL_FILES: 'Local Files'
};

const PAGE_SIZE = 10;

const PROTEIN_DATA_BANK_COLUMNS = ['source', 'id', 'method', 'resolution', 'chain', 'positions', 'links'];

export default class ngbStructurePanelService {

    get sourceOptions() {
        return SOURCE_OPTIONS;
    }
    get pageSize() {
        return PAGE_SIZE;
    }
    get proteinDataBankColumns() {
        return PROTEIN_DATA_BANK_COLUMNS;
    }

    _totalPages = 0;
    _currentPage = 1;
    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _emptyResults = false;
    _structureResults = null;

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
    get errorMessageList() {
        return this._errorMessageList;
    }
    get emptyResults() {
        return this._emptyResults;
    }
    get structureResults() {
        return this._structureResults;
    }

    get sourceModel() {
        return this._sourceModel;
    }
    set sourceModel(value) {
        this._sourceModel = value;
    }

    static instance (targetDataService) {
        return new ngbStructurePanelService(targetDataService);
    }

    constructor(targetDataService) {
        Object.assign(this, {targetDataService});
        this._sourceModel = this.sourceOptions.PROTEIN_DATA_BANK;
    }

    setStructureResults(data) {

    }

    getStructureResults() {
        const request = {};
        if (!request) {
            return new Promise(resolve => {
                this._loadingData = false;
                resolve(true);
            });
        }
        const source = this.sourceModel;
        return new Promise(resolve => {
            this.targetDataService.getStructureResults(request, source)
                .then(([data, totalCount]) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyResults = totalCount === 0;
                    this.setStructureResults(data);
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

    resetStructureData() {
        this._totalPages = 0;
        this._currentPage = 1;
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
        this._structureResults = null;
    }
}
