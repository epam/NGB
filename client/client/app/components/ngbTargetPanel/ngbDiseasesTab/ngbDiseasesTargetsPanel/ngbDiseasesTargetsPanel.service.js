const PAGE_SIZE = 10;

const fixedNumber = (num) => {
    if (!num) {
        return num;
    }
    const fixed = Number(num.toFixed(2));
    return fixed ? fixed : undefined;
};

const FIELDS = {
    'target': 'GENE_ID',
    'target name': 'DISEASE_NAME',
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

    static instance (ngbDiseasesTabService, targetDataService) {
        return new ngbDiseasesTargetsPanelService(ngbDiseasesTabService, targetDataService);
    }

    constructor(ngbDiseasesTabService, targetDataService) {
        Object.assign(this, {ngbDiseasesTabService, targetDataService});
    }

    get diseaseId() {
        return this.ngbDiseasesTabService.diseasesData.id;
    }

    setFieldList() {}

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
                target: item.diseaseId,
                'target name': item.diseaseName,
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
        return request;
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
}