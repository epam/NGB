const SOURCE_OPTIONS = {
    PROTEIN_DATA_BANK: 'Protein Data Bank',
    LOCAL_FILES: 'Local Files'
};

const PAGE_SIZE = 10;

const PROTEIN_DATA_BANK_COLUMNS = ['id', 'name', 'source', 'resolution', 'chains'];

const FIELDS = {
    id: 'ENTRY_ID',
    resolution: 'RESOLUTION',
    name: 'NAME'
};

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

    get fields() {
        return FIELDS;
    }

    _totalPages = 0;
    _currentPage = 1;
    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _emptyResults = false;
    _structureResults = null;
    _filterInfo = null;
    _selectedPdbId = null;

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

    get filterInfo() {
        return this._filterInfo;
    }
    set filterInfo(value) {
        this._filterInfo = value;
    }

    get selectedPdbId() {
        return this._selectedPdbId;
    }
    set selectedPdbId(value) {
        this._selectedPdbId = value;
    }

    static instance (ngbTargetPanelService, targetDataService) {
        return new ngbStructurePanelService(ngbTargetPanelService, targetDataService);
    }

    constructor(ngbTargetPanelService, targetDataService) {
        Object.assign(this, {ngbTargetPanelService, targetDataService});
        this._sourceModel = this.sourceOptions.PROTEIN_DATA_BANK;
    }

    get geneIds() {
        return this.ngbTargetPanelService.genesIds;
    }

    setStructureResults(data) {
        this._structureResults = data.map(item => ({
            id: {
                name: item.id,
                url: item.url
            },
            name: item.name,
            source: item.source,
            resolution: item.resolution,
            chains: (item.proteinChains || []).join('/')
        }))
    }

    getStructureRequest () {
        const request = {
            geneIds: this.geneIds,
            page: this.currentPage,
            pageSize: this.pageSize,
            orderBy: this.fields.id,
            reverse: false
        };
        if (this._filterInfo) {
            if (this._filterInfo.id) {
                request.entryIds = [this._filterInfo.id];
            }
        }
        return request;
    }

    getStructureResults() {
        const request = this.getStructureRequest();
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

    setFilter(field, value) {
        const filter = {...(this._filterInfo || {})};
        if (value) {
            filter[field] = value;
        } else {
            delete filter[field];
        }
        this._filterInfo = filter;
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
