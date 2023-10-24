import {SourceOptions} from '../ngbDiseasesPanel.service';
const PAGE_SIZE = 10;

const fixedNumber = (num) => {
    if (!num) {
        return num;
    }
    const fixed = Number(num.toFixed(2));
    return fixed ? fixed : undefined;
};

const FIELDS = {
    OPEN_TARGETS: {
        'target': 'GENE_ID',
        'disease': 'DISEASE_NAME',
        'overall score': 'OVERALL_SCORE',
        'genetic association': 'GENETIC_ASSOCIATIONS_SCORE',
        'somatic mutations': 'SOMATIC_MUTATIONS_SCORE',
        'drugs': 'DRUGS_SCORE',
        'pathways systems': 'PATHWAYS_SCORE',
        'text mining': 'TEXT_MINING_SCORE',
        'animal models': 'ANIMAL_MODELS_SCORE',
        'RNA expression': 'RNA_EXPRESSION_SCORE'
    },
    PHARM_GKB: {
        'target': 'GENE_ID',
        'disease': 'DISEASE_NAME'
    }
};

export default class ngbDiseasesTableService {

    _diseasesResults = null;
    _totalPages = 0;
    _currentPage = 1;
    _sortInfo = null;
    _filterInfo = null;
    fieldList = {};

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
        return this.ngbDiseasesPanelService
            ? this.ngbDiseasesPanelService.tableLoading
            : false;
    }
    set loadingData(value) {
        if (this.ngbDiseasesPanelService) {
            this.ngbDiseasesPanelService.tableLoading = value;
        }
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
    get diseasesResults() {
        return this._diseasesResults;
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

    get fields () {
        return FIELDS;
    }

    static instance (
        dispatcher,
        ngbTargetPanelService,
        ngbDiseasesPanelService,
        targetDataService
    ) {
        return new ngbDiseasesTableService(
            dispatcher,
            ngbTargetPanelService,
            ngbDiseasesPanelService,
            targetDataService
        );
    }

    constructor(
        dispatcher,
        ngbTargetPanelService,
        ngbDiseasesPanelService,
        targetDataService
    ) {
        Object.assign(this, {
            dispatcher,
            ngbTargetPanelService,
            ngbDiseasesPanelService,
            targetDataService
        });
        this.dispatcher.on('target:identification:reset', this.resetDiseasesData.bind(this));
    }

    get identificationTarget() {
        return this.ngbTargetPanelService.identificationTarget || {};
    }

    get sourceModel () {
        return this.ngbDiseasesPanelService.sourceModel;
    }

    get geneIds() {
        return [...this.ngbTargetPanelService.allGenes.map(i => i.geneId)];
    }

    getTarget(id) {
        if (!id) return;
        return this.ngbTargetPanelService.getChipByGeneId(id);
    }

    setDiseasesResult(result) {
        if (this.sourceModel === SourceOptions.OPEN_TARGETS) {
            this._diseasesResults = result.map(item => {
                const {
                    OVERALL,
                    GENETIC_ASSOCIATIONS,
                    SOMATIC_MUTATIONS,
                    DRUGS,
                    PATHWAYS,
                    TEXT_MINING,
                    ANIMAL_MODELS,
                    RNA_EXPRESSION
                } = item.scores;

                return {
                    target: this.getTarget(item.geneId),
                    disease: item.disease,
                    'overall score': fixedNumber(OVERALL),
                    'genetic association': fixedNumber(GENETIC_ASSOCIATIONS),
                    'somatic mutations': fixedNumber(SOMATIC_MUTATIONS),
                    'drugs': fixedNumber(DRUGS),
                    'pathways systems': fixedNumber(PATHWAYS),
                    'text mining': fixedNumber(TEXT_MINING),
                    'animal models': fixedNumber(ANIMAL_MODELS),
                    'RNA expression': fixedNumber(RNA_EXPRESSION)
                };
            });
        }
        if (this.sourceModel === SourceOptions.PHARM_GKB) {
            this._diseasesResults = result.map(item => {
                return {
                    target: this.getTarget(item.geneId),
                    disease: {
                        id: item.id,
                        name: item.name,
                        url: item.url
                    }
                };
            });
        }
    }

    getRequest() {
        const request = {
            page: this.currentPage,
            pageSize: this.pageSize,
            geneIds: this.geneIds,
        };
        if (this.sortInfo && this.sortInfo.length) {
            request.orderInfos = this.sortInfo.map(i => ({
                orderBy: this.fields[this.sourceModel][i.field],
                reverse: !i.ascending
            }))
        }
        if (this._filterInfo) {
            const filters = Object.entries(this._filterInfo)
                .filter(([key, values]) => values.length)
                .map(([key, values]) => {
                    const filter = {
                        field: this.fields[this.sourceModel][key]
                    };
                    switch (key) {
                        case 'target':
                            filter.terms = values.map(v => {
                                const chip = this.ngbTargetPanelService.getGeneIdByChip(v);
                                return chip ? chip : '';
                            });
                            return filter;
                        case 'disease':
                            filter.terms = Array.isArray(values) ? values.map(v => v) : [values];
                            return filter;
                        default:
                            filter.range = {
                                from: Number(values),
                                to: '1.0'
                            }
                            return filter;
                    }
                });
            if (filters && filters.length) {
                request.filters = filters;
            }
        }
        return request;
    }

    getDiseasesResults() {
        const request = this.getRequest();
        if (!request.geneIds || !request.geneIds.length) {
            return new Promise(resolve => {
                this.loadingData = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getDiseasesResults(request, this.sourceModel)
                .then(([data, totalCount]) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyResults = totalCount === 0;
                    this.setDiseasesResult(data);
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

    setFieldList() {
        const allGenes = this.ngbTargetPanelService.allGenes;
        this.fieldList = {
            target: [...allGenes.map(i => i.chip)],
            disease: []
        };
        this.dispatcher.emitSimpleEvent('diseases:filters:list');
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

    resetDiseasesData() {
        this._diseasesResults = null;
        this._currentPage = 1;
        this._totalPages = 0;
        this._sortInfo = null;
        this._filterInfo = null;
        this.loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
    }
}
