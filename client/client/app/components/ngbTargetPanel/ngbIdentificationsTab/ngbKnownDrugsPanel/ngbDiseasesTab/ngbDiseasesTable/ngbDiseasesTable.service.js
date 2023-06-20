const PAGE_SIZE = 10;

const fixedNumber = (num) => {
    if (!num) {
        return num;
    }
    const fixed = Number(num.toFixed(2));
    return fixed ? fixed : undefined;
};

export default class ngbDiseasesTableService {

    _diseasesResults = null;
    _totalPages = 0;
    _currentPage = 1;
    _sortInfo = null;

    _loadingData = false;
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

    static instance (
        dispatcher,
        ngbTargetPanelService,
        ngbKnownDrugsPanelService,
        targetDataService
    ) {
        return new ngbDiseasesTableService(
            dispatcher,
            ngbTargetPanelService,
            ngbKnownDrugsPanelService,
            targetDataService
        );
    }

    constructor(
        dispatcher,
        ngbTargetPanelService,
        ngbKnownDrugsPanelService,
        targetDataService
    ) {
        Object.assign(this, {
            dispatcher,
            ngbTargetPanelService,
            ngbKnownDrugsPanelService,
            targetDataService
        });
        this.dispatcher.on('reset:identification:data', this.resetDiseasesData.bind(this));
    }

    get identificationTarget() {
        return this.ngbTargetPanelService.identificationTarget || {};
    }

    get geneIds() {
        const {interest, translational} = this.identificationTarget;
        return [...interest.map(i => i.geneId), ...translational.map(t => t.geneId)];
    }

    getTarget(id) {
        if (!id) return;
        const {interest, translational} = this.identificationTarget;
        const genes = [...interest, ...translational]
            .filter(gene => gene.geneId === id)
            .map(gene => gene.chip);
        if (genes && genes.length) {
            return genes[0];
        }
    }

    setDiseasesResult(result) {
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
                target: this.getTarget(item.targetId),
                disease: item.disease,
                'overall score': fixedNumber(OVERALL),
                'genetic association': fixedNumber(GENETIC_ASSOCIATIONS),
                'somatic mutations': fixedNumber(SOMATIC_MUTATIONS),
                'drugs': fixedNumber(DRUGS),
                'pathways systems': fixedNumber(PATHWAYS),
                'text mining': fixedNumber(TEXT_MINING),
                'animal models': fixedNumber(ANIMAL_MODELS),
                'rna expression': fixedNumber(RNA_EXPRESSION)
            };
        });
    }

    getRequest() {
        return {
            page: this.currentPage,
            pageSize: this.pageSize,
            geneIds: this.geneIds,
        };
    }

    getDiseasesResults() {
        const request = this.getRequest();
        const source = this.ngbKnownDrugsPanelService.sourceModel.name;
        return new Promise(resolve => {
            this.targetDataService.getDiseasesResults(request, source)
                .then(([data, totalCount]) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyResults = totalCount === 0;
                    this.setDiseasesResult(data);
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

    resetDiseasesData() {
        this._diseasesResults = null;
        this._currentPage = 1;
        this._totalPages = 0;
        this._sortInfo = null;
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
    }
}
