const PAGE_SIZE = 10;

const fixedNumber = (num) => {
    if (!num) {
        return num;
    }
    const fixed = Number(num.toFixed(2));
    return fixed ? fixed : undefined;
};

const FIELDS = {
    'target': 'GENE_SYMBOL',
    'target name': 'GENE_NAME',
    'overall score': 'OVERALL_SCORE',
    'genetic association': 'GENETIC_ASSOCIATIONS_SCORE',
    'somatic mutations': 'SOMATIC_MUTATIONS_SCORE',
    'drugs': 'DRUGS_SCORE',
    'pathways systems': 'PATHWAYS_SCORE',
    'text mining': 'TEXT_MINING_SCORE',
    'animal models': 'ANIMAL_MODELS_SCORE',
    'RNA expression': 'RNA_EXPRESSION_SCORE',
};

export default class ngbDiseasesTargetsPanelService {

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
    _targetsResults = null;

    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
    }
    get failedResult () {
        return this._failedResult;
    }
    get errorMessageList () {
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
    get targetsResults() {
        return this._targetsResults;
    }

    static instance (dispatcher, ngbDiseasesTabService, targetDataService) {
        return new ngbDiseasesTargetsPanelService(dispatcher, ngbDiseasesTabService, targetDataService);
    }

    constructor(dispatcher, ngbDiseasesTabService, targetDataService) {
        Object.assign(this, {dispatcher, ngbDiseasesTabService, targetDataService});
        dispatcher.on('target:diseases:disease:changed', this.resetData.bind(this));
    }

    get diseaseId() {
        return this.ngbDiseasesTabService.diseasesData.id;
    }

    setFilter(field, value) {
        const filter = {...(this._filterInfo || {})};
        if (value && value.length) {
            filter[field] = value;
        } else {
            delete filter[field];
        }
        this._filterInfo = filter;
    }

    setTargetsResults(results) {
        this._targetsResults = results.map(item => {
            const {
                overallScore,
                geneticAssociationScore,
                somaticMutationScore,
                knownDrugScore,
                affectedPathwayScore,
                literatureScore,
                animalModelScore,
                rnaExpressionScore
            } = item;
            return {
                'target': item.geneSymbol,
                'target name': item.geneName,
                'overall score': fixedNumber(overallScore),
                'genetic association': fixedNumber(geneticAssociationScore),
                'somatic mutations': fixedNumber(somaticMutationScore),
                'drugs': fixedNumber(knownDrugScore),
                'pathways systems': fixedNumber(affectedPathwayScore),
                'text mining': fixedNumber(literatureScore),
                'animal models': fixedNumber(animalModelScore),
                'rna expression': fixedNumber(rnaExpressionScore)
            };
        });
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
        if (this._filterInfo) {
            const filters = Object.entries(this._filterInfo)
                .filter(([key, values]) => values.length)
                .map(([key, values]) => {
                    return {
                        field: this.fields[key],
                        terms: values.map(v => v)
                    };
                });
            if (filters && filters.length) {
                request.filters = filters;
            }
        }
        return request;
    }

    setDiseasesData(totalTargets) {
        const data = this.ngbDiseasesTabService.diseasesData;
        data.targetsCount = totalTargets;
        this.ngbDiseasesTabService.diseasesData = data;
        this.dispatcher.emit('target:diseases:targets:count:updated');
    }

    getTargetsResults() {
        const request = this.getRequest();
        if (!this.diseaseId) {
            return new Promise(resolve => {
                this.loadingData = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getTargetsResults(this.diseaseId, request)
                .then(([data, totalCount]) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyResults = totalCount === 0;
                    this.setTargetsResults(data);
                    this.setDiseasesData(totalCount);
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
        this._targetsResults = null;
    }

    exportResults() {
        if (!this.diseaseId) {
            return new Promise(resolve => {
                resolve(true);
            });
        }
        const source = 'targets';
        return this.targetDataService.getDiseasesExport(this.diseaseId, source);
    }
}
