const PAGE_SIZE = 30;

export default class ngbTargetTableService {
    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;

    _emptyResults = false;
    _filteringFailure = false;
    _filteringErrorMessageList = null;

    _displayFilters = false;
    _filterInfo = null;

    _totalCount = 0;
    _currentPage = 1;
    _pageSize = PAGE_SIZE

    targetsResults = null;

    get pageSize() {
        return this._pageSize;
    }
    set pageSize(value) {
        this._pageSize = value;
    }
    get totalCount() {
        return this._totalCount;
    }
    get currentPage() {
        return this._currentPage;
    }
    set currentPage(value) {
        this._currentPage = value;
    }
    get isFirstPage () {
        return this._currentPage === 1;
    }
    get isLastPage () {
        return this._currentPage === this._totalCount;
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

    get errorMessageList () {
        return this._errorMessageList;
    }
    set errorMessageList (value) {
        this._errorMessageList = value;
    }

    get displayFilters() {
        return this._displayFilters;
    }
    set displayFilters(value) {
        this._displayFilters = value;
    }

    get filterInfo() {
        return this._filterInfo;
    }

    get emptyResults() {
        return this._emptyResults;
    }
    get filteringFailure() {
        return this._filteringFailure;
    }
    get filteringErrorMessageList() {
        return this._filteringErrorMessageList;
    }

    static instance (targetDataService) {
        return new ngbTargetTableService(targetDataService);
    }

    constructor(targetDataService) {
        Object.assign(this, {targetDataService});
    }

    setGetTargetsRequest() {
        const request = {
            pagingInfo: {
                pageSize: this.pageSize,
                pageNum: this.currentPage
            },
        };
        if (this._filterInfo) {
            if (this._filterInfo.name) {
                request.targetName = this._filterInfo.name;
            }
            if (this._filterInfo.genes) {
                request.geneNames = [this._filterInfo.genes];
            }
            if (this._filterInfo.species) {
                request.speciesNames = [this._filterInfo.species];
            }
            if (this._filterInfo.disease) {
                request.diseases = [this._filterInfo.disease];
            }
            if (this._filterInfo.product) {
                request.products = [this._filterInfo.product];
            }
        }
        return request;
    }

    setTargetsResult(data) {
        this.targetsResults = [...(data || []).map(item => (
            {
                id: item.targetId,
                name: item.targetName,
                genes: {
                    value: item.targetGenes.map(gene => gene.geneName),
                    limit: 2
                },
                species: {
                    value: item.targetGenes.map(gene => gene.speciesName),
                    limit: 2
                },
                disease: {
                    value: item.diseases,
                    limit: 2
                },
                product: {
                    value: item.products,
                    limit: 2
                }
            }
        ))];
    }

    getTargetsResult(request) {
        const isFiltered = JSON.stringify(this.filterInfo) !== '{}';
        return new Promise(resolve => {
            this.targetDataService.getTargetsResult(request)
                .then(([data, totalCount]) => {
                    this._errorMessageList = null;
                    this._failedResult = false;
                    this._totalCount = Math.ceil(totalCount/this.pageSize);
                    this._emptyResults = this._totalCount === 0 ? (isFiltered ? false : true) : false;
                    this._filteringFailure = false;
                    this._filteringErrorMessageList = null;
                    this.setTargetsResult(data);
                    resolve(true);
                })
                .catch(err => {
                    this._totalCount = 0;
                    this.targetsResults = null;
                    this._currentPage = 1;
                    if (isFiltered) {
                        this._errorMessageList = null;
                        this._failedResult = false;
                        this._emptyResults = true;
                        this._filteringFailure = true;
                        this._filteringErrorMessageList = [err.message];
                        resolve(true);
                    } else {
                        this._errorMessageList = [err.message];
                        this._failedResult = true;
                        this._emptyResults = false;
                        this._filteringFailure = false;
                        this._filteringErrorMessageList = null;
                        resolve(false);
                    }
                });
        });
    }

    setFilter(field, string) {
        const filter = {...(this._filterInfo || {})};
        filter[field] = string;
        this._filterInfo = filter;
    }
}
