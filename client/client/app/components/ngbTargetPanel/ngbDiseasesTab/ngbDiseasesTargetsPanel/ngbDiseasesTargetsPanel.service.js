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
    _speciesFilter = null;

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

    static instance (dispatcher, ngbDiseasesTabService, targetDataService, utilsDataService) {
        return new ngbDiseasesTargetsPanelService(dispatcher, ngbDiseasesTabService, targetDataService, utilsDataService);
    }

    constructor(dispatcher, ngbDiseasesTabService, targetDataService, utilsDataService) {
        Object.assign(this, {dispatcher, ngbDiseasesTabService, targetDataService, utilsDataService});
        dispatcher.on('target:diseases:disease:changed', this.resetData.bind(this));
    }

    get diseaseId() {
        return (this.ngbDiseasesTabService.diseasesData || {}).id;
    }

    async getTargetSettings() {
        const {target_settings: targetSettings} = await this.utilsDataService.getDefaultTrackSettings();
        if (!targetSettings) return;
        const {species_filter: speciesFilter} = targetSettings;
        if (!speciesFilter) return;
        this.setSpeciesFilter(speciesFilter);
    }

    setSpeciesFilter(speciesFilter) {
        this._speciesFilter = Object.entries(speciesFilter).map(([taxId, name]) => ({ name, taxId }));
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

    getAllHomologues(homologues) {
        if (!homologues) return [];
        let homologuesArray = Array.from(new Set(
                homologues.map(h => JSON.stringify({
                    name: h.speciesCommonName,
                    taxId: h.taxId
                }))
            )).map(h => JSON.parse(h));
        if (this._filterInfo && this._filterInfo.homologues) {
            const taxIds = this._filterInfo.homologues.map(h => h.taxId);
            homologuesArray = homologuesArray.filter(h => {
                return taxIds.includes(`${h.taxId}`);
            })
        }
        return homologuesArray;
    }

    setTargetsResults(results) {
        this._targetsResults = results.map(item => {
            const {
                geneSymbol,
                geneName,
                homologues,
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
                'target': geneSymbol,
                'target name': geneName,
                'homologues': {
                    value: this.getAllHomologues(homologues),
                    limit: 2
                },
                'overall score': fixedNumber(overallScore),
                'genetic association': fixedNumber(geneticAssociationScore),
                'somatic mutations': fixedNumber(somaticMutationScore),
                'drugs': fixedNumber(knownDrugScore),
                'pathways systems': fixedNumber(affectedPathwayScore),
                'text mining': fixedNumber(literatureScore),
                'animal models': fixedNumber(animalModelScore),
                'RNA expression': fixedNumber(rnaExpressionScore)
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
                .filter(([key, values]) => values && values.length)
                .map(([key, values]) => {
                    const filter = {
                        field: this.fields[key],
                    };
                    switch (key) {
                        case 'homologues':
                            break;
                        case 'target':
                        case 'target name':
                            filter.terms = Array.isArray(values) ? values.map(v => v) : [values];
                            return filter;
                        default:
                            filter.range = {
                                from: Number(values),
                                to: '1.0'
                            };
                            return filter;
                    }
                })
                .filter(i => i);
            if (filters && filters.length) {
                request.filters = filters;
            }
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

    async setDefaultFilter() {
        await this.getTargetSettings();
        if (!this._speciesFilter || !this._speciesFilter.length) return;
        this.setFilter('homologues', this._speciesFilter);
    }

    async setFieldList() {
        if (!this._speciesFilter) {
            this.fieldList = {};
            this.dispatcher.emitSimpleEvent('target:diseases:targets:filters:list');
            return;
        }
        this.fieldList.homologues = this._speciesFilter;
        this.dispatcher.emitSimpleEvent('target:diseases:targets:filters:list')
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
        this._speciesFilter = null;
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
