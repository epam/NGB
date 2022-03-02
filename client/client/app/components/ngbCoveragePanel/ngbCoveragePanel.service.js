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
    _isFilteredSearchFailure = false;
    _filteredErrorMessageList = null;
    _displayFilters = false;

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
    get sortInfo() {
        return this._sortInfo;
    }
    set sortInfo(value) {
        this._sortInfo = value;
    }
    get filterInfo() {
        return this._filterInfo;
    }

    get currentCoverageIndex() {
        return this._currentCoverageIndex;
    }
    set currentCoverageIndex(value) {
        this._currentCoverageIndex = value;
    }
    get isFilteredSearchFailure() {
        return this._isFilteredSearchFailure;
    }
    get filteredErrorMessageList() {
        return this._filteredErrorMessageList;
    }

    get displayFilters() {
        return this._displayFilters;
    }
    set displayFilters(value) {
        this._displayFilters = value;
    }

    static instance (
        appLayout,
        dispatcher,
        bamDataService,
        bamCoverageContext,
        projectContext
    ) {
        return new ngbCoveragePanelService(
            appLayout,
            dispatcher,
            bamDataService,
            bamCoverageContext,
            projectContext
        );
    }

    constructor(appLayout, dispatcher, bamDataService, bamCoverageContext, projectContext) {
        Object.assign(this, {appLayout, dispatcher, bamDataService, bamCoverageContext, projectContext});
        this.dispatcher.on('reference:change', this.panelCloseCoveragePanel.bind(this));
        this.dispatcher.on('bam:coverage:empty', this.panelCloseCoveragePanel.bind(this));
        this.dispatcher.on('coverage:filters:reset', this.resetFilters.bind(this));
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
        const isFiltered = JSON.stringify(this.filterInfo) !== '{}';
        return new Promise(resolve => {
            this.bamDataService.searchBamCoverage(request)
                .then(([data, totalCount]) => {
                    this._errorMessageList = null;
                    this._totalCount = totalCount;
                    this._emptyResults = totalCount === 0 ? (isFiltered ? false : true) : false;
                    this._isFilteredSearchFailure = false;
                    this._filteredErrorMessageList = null;
                    this.coverageSearchResults = data;
                    resolve(true);
                })
                .catch(err => {
                    this._totalCount = 0;
                    this.coverageSearchResults = null;
                    if (isFiltered) {
                        this._errorMessageList = null;
                        this._emptyResults = true;
                        this._isFilteredSearchFailure = true;
                        this._filteredErrorMessageList = [err.message];
                        resolve(true);
                    } else {
                        this._errorMessageList = [err.message];
                        this._emptyResults = false;
                        this._isFilteredSearchFailure = false;
                        this._filteredErrorMessageList = null;
                        resolve(false);
                    }
                });
        });
    }

    setFilter (key, value) {
        const filter = {...(this._filterInfo || {})};
        filter[key] = value;
        if (key === 'chromosomes' && !value.length) {
            delete filter[key];
        }
        if (value.from === null && value.to === null) {
            delete filter[key];
        }
        this._filterInfo = filter;
        this.bamCoverageContext.isFiltersDefault = JSON.stringify(filter) === '{}';
    }

    resetCurrentPages() {
        this._currentPages = {
            first: 0,
            last: 0
        };
    }

    resetFilters() {
        this.clearFilters();
        this.dispatcher.emitSimpleEvent('coverage:filter:changed');
    }

    clearFilters() {
        this._filterInfo = null;
        this.bamCoverageContext.isFiltersDefault = true;
    }

    resetInfo() {
        this._errorMessageList = null;
        this._totalCount = 0;
        this._emptyResults = false;
        this._currentCoverageIndex = null;
        this.coverageSearchResults = null;
        this._isFilteredSearchFailure = false;
        this._filteredErrorMessageList = null;
        this.resetCurrentPages();
        this._sortInfo = null;
        this.clearFilters();
        this._displayFilters = false;
    }
}
