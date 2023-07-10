const PAGE_SIZE = 30;

const FIELDS = {
    name: 'NAME',
    genes: 'GENE_NAME',
    species: 'SPECIES_NAME',
    diseases: 'DISEASES',
    products: 'PRODUCTS'
};

export default class ngbTargetsTableService {

    _emptyResults = false;
    _filteringFailure = false;
    _filteringErrorMessageList = null;

    _displayFilters = false;

    _filterInfo = null;
    _sortInfo = null;

    _totalCount = 0;
    _currentPage = 1;
    _pageSize = PAGE_SIZE;

    targetsResults = null;

    fields = FIELDS;

    fieldList = {};

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

    get displayFilters() {
        return this._displayFilters;
    }
    set displayFilters(value) {
        this._displayFilters = value;
    }

    get filterInfo() {
        return this._filterInfo;
    }

    get sortInfo() {
        return this._sortInfo;
    }
    set sortInfo(value) {
        this._sortInfo = value;
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

    static instance (projectContext, dispatcher, ngbTargetsTabService, targetDataService) {
        return new ngbTargetsTableService(projectContext, dispatcher, ngbTargetsTabService, targetDataService);
    }

    constructor(projectContext, dispatcher, ngbTargetsTabService, targetDataService) {
        Object.assign(this, {projectContext, dispatcher, ngbTargetsTabService, targetDataService});
        this.dispatcher.on('targets:filters:reset', this.resetFilters.bind(this));
    }

    get failedResult() {
        return this.ngbTargetsTabService.tableFailed;
    }
    set failedResult(value) {
        this.ngbTargetsTabService.tableFailed = value;
    }
    get errorMessageList () {
        return this.ngbTargetsTabService.tableErrorMessageList;
    }
    set errorMessageList (value) {
        this.ngbTargetsTabService.tableErrorMessageList = value;
    }

    setGetTargetsRequest() {
        const request = {
            pagingInfo: {
                pageSize: this.pageSize,
                pageNum: this.currentPage
            }
        };
        if (this._sortInfo && this._sortInfo.length) {
            request.sortInfo = {
                field: 'target_name',
                ascending: this._sortInfo[0].ascending
            };
        }
        if (this._filterInfo) {
            if (this._filterInfo.name) {
                request.targetName = this._filterInfo.name;
            }
            if (this._filterInfo.genes) {
                request.geneNames = [...this._filterInfo.genes];
            }
            if (this._filterInfo.species) {
                request.speciesNames = [...this._filterInfo.species];
            }
            if (this._filterInfo.diseases) {
                request.diseases = [...this._filterInfo.diseases];
            }
            if (this._filterInfo.products) {
                request.products = [...this._filterInfo.products];
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
                    value: item.targetGenes.map(gene => ({
                        speciesName: gene.speciesName,
                        taxId: gene.taxId,
                        geneName: gene.geneName,
                        geneId: gene.geneId
                    })),
                    limit: 2
                },
                diseases: {
                    value: item.diseases,
                    limit: 2
                },
                products: {
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
                    this.errorMessageList = null;
                    this.failedResult = false;
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
                        this.errorMessageList = null;
                        this.failedResult = false;
                        this._emptyResults = true;
                        this._filteringFailure = true;
                        this._filteringErrorMessageList = [err.message];
                        resolve(true);
                    } else {
                        this.errorMessageList = [err.message];
                        this.failedResult = true;
                        this._emptyResults = false;
                        this._filteringFailure = false;
                        this._filteringErrorMessageList = null;
                        resolve(false);
                    }
                });
        });
    }

    get isFilterEmpty() {
        if (!this._filterInfo) {
            return true;
        }
        return Object.values(this._filterInfo).every(filter => !filter.length);
    }

    setFilter(field, value) {
        const filter = {...(this._filterInfo || {})};
        if (value) {
            filter[field] = value;
        } else {
            delete filter[field];
        }
        this._filterInfo = filter;
        this.projectContext.targetsTableFilterIsVisible = !this.isFilterEmpty;
    }

    resetFilters() {
        this._filterInfo = null;
        this.projectContext.targetsTableFilterIsVisible = false;
        this.dispatcher.emitSimpleEvent('targets:filters:changed');
    }

    async onChangeShowFilters() {
        if (this.displayFilters) {
            return Promise.all(
                ['genes', 'species', 'diseases', 'products'].map(async (field) => (
                    await this.getTargetFieldValue(field)
                )))
                    .then(values => (values.some(v => v)));
        }
    }


    getTargetFieldValue(field) {
        return new Promise(resolve => {
            this.targetDataService.getTargetFieldValue(this.fields[field])
                .then((data) => {
                    this.fieldList[field] = data.filter(d => d);
                    resolve(true);
                })
                .catch(err => {
                    this.fieldList[field] = [];
                    resolve(false);
                });
        });
    }
}
