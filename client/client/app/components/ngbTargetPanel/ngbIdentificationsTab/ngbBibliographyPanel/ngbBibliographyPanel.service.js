const PAGE_SIZE = 5;

export default class ngbBibliographyPanelService {

    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _emptyResults = false;

    _publicationsResults = null;
    _totalPages = 0;
    _currentPage = 1;

    get pageSize() {
        return PAGE_SIZE;
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

    get publicationsResults() {
        return this._publicationsResults;
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
    
    static instance (ngbTargetPanelService, targetDataService) {
        return new ngbBibliographyPanelService(ngbTargetPanelService, targetDataService);
    }

    constructor(ngbTargetPanelService, targetDataService) {
        Object.assign(this, {ngbTargetPanelService, targetDataService});
    }

    get geneIds() {
        return this.ngbTargetPanelService.allGenes.map(g => g.geneId);
    }

    getRequest() {
        return {
            geneIds: this.geneIds
        }
    }

    getPublicationsResults() {
        const request = this.getRequest();
        if (!request.geneIds || !request.geneIds.length) {
            return new Promise(resolve => {
                this._loadingData = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getPublications(request)
                .then(([data, totalCount]) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyResults = totalCount === 0;
                    this._publicationsResults = data.slice(0, 3);
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._totalPages = 0;
                    this._emptyResults = false;
                    this._loadingData = false;
                    console.log(this._failedResult, this._errorMessageList)
                    resolve(false);
                });
        });
    }
}
