const PAGE_SIZE = 100;

export default class ngbCoveragePanelService {

    _errorMessageList = null;
    _emptyResults = false;
    _totalCount = 0;
    _currentPages = {
        first: 0,
        last: 0
    };
    _sortInfo = null;
    _filterInfo = null;
    _currentCoverageIndex;

    coverageSearchResults = null;

    get pageSize() {
        return PAGE_SIZE;
    }
    get errorMessageList() {
        return this._errorMessageList;
    }
    get emptyResults() {
        return this._emptyResults;
    }
    get totalCount() {
        return this._totalCount;
    }
    get currentPages() {
        return this._currentPages;
    }

    get currentCoverageIndex() {
        return this._currentCoverageIndex;
    }
    set currentCoverageIndex(value) {
        this._currentCoverageIndex = value;
    }

    static instance (appLayout, dispatcher, bamDataService) {
        return new ngbCoveragePanelService(appLayout, dispatcher, bamDataService);
    }

    constructor(appLayout, dispatcher, bamDataService) {
        Object.assign(this, {appLayout, dispatcher, bamDataService});
        this.dispatcher.on('reference:change', this.panelCloseCoveragePanel.bind(this));
        this.dispatcher.on('bam:coverage:empty', this.panelCloseCoveragePanel.bind(this));
    }

    panelCloseCoveragePanel () {
        this.resetInfo();
        const layoutChange = this.appLayout.Panels.coverage;
        layoutChange.displayed = false;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    getNeededPage(isScrollTop) {
        if (isScrollTop) {
            this._currentPages.last = this._currentPages.first;
            this._currentPages.first -= 1;
            return  this._currentPages.first;
        } else {
            this._currentPages.first = this._currentPages.last;
            this._currentPages.last += 1;
            return this._currentPages.last;
        }
    }

    async setSearchCoverageRequest(index, isScrollTop) {
        const neededPage = this.getNeededPage(isScrollTop);
        const request = {
            pagingInfo: {
                pageSize: this.pageSize,
                pageNum: neededPage
            },
            sortInfo: this._sortInfo,
            coverageId: index
        };
        if (this._filterInfo) {
            Object.assign(request, this._filterInfo);
        }
        return request;
    }

    searchBamCoverage(request) {
        return new Promise(resolve => {
            this.bamDataService.searchBamCoverage(request)
                .then(([data, totalCount]) => {
                    this._errorMessageList = null;
                    this._totalCount = totalCount;
                    this._emptyResults = totalCount === 0;
                    this.coverageSearchResults = data;
                    resolve(true);
                })
                .catch(err => {
                    this._errorMessageList = [err.message];
                    this._totalCount = 0;
                    this._emptyResults = false;
                    this.coverageSearchResults = null;
                    resolve(false);
                });
        });
    }

    resetCurrentPages() {
        this._currentPages = {
            first: 0,
            last: 0
        };
    }

    resetInfo() {
        this._errorMessageList = null;
        this._emptyResults = false;
        this._totalCount = 0;
        this._sortInfo = null;
        this._filterInfo = null;
        this.coverageSearchResults = null;
        this._currentCoverageIndex = null;
        this.resetCurrentPages();
    }
}
