const PAGE_SIZE = 100;
const ROW_HEIGHT = 35;
const REFERENCE = 'REFERENCE';
const CHROMOSOME = 'CHROMOSOME';
const WHOLE_GENOME = 'WHOLE_GENOME';
const POSITIVE_STRAND = '+';
const NEGATIVE_STRAND = '-';
const POSITIVE = 'POSITIVE';
const NEGATIVE = 'NEGATIVE';

export default class ngbMotifsPanelService {

    searchMotifResults = null;
    _searchMotifsParams = [];
    motifsResultsTitle = null;
    _searchStopOn = {};
    _currentParams = {};
    searchRequestsHistory = [];
    requestNumber = 0;

    _isSearchInProgress = false;
    _isSearchFailure = false;
    _errorMessageList = null;
    _isShowParamsTable = false;

    get isSearchInProgress () {
        return this._isSearchInProgress;
    }
    set isSearchInProgress (value) {
        this._isSearchInProgress = value;
    }

    get isSearchFailure () {
        return this._isSearchFailure;
    }
    set isSearchFailure (value) {
        this._isSearchFailure = value;
    }

    get errorMessageList () {
        return this._errorMessageList;
    }
    set errorMessageList (error) {
        this._errorMessageList = error;
    }

    get isShowParamsTable () {
        return this._isShowParamsTable;
    }
    set isShowParamsTable (value) {
        this._isShowParamsTable = value;
    }

    get searchMotifsParams () {
        return [...this._searchMotifsParams].reverse();
    }

    get searchStopOn () {
        return this._searchStopOn;
    }
    set searchStopOn (value) {
        this._searchStopOn = value;
    }

    get currentParams () {
        return this._currentParams;
    }

    get pageSize () {
        return PAGE_SIZE;
    }
    get rowHeight () {
        return ROW_HEIGHT;
    }
    get referenceType () {
        return REFERENCE;
    }
    get chromosomeType () {
        return CHROMOSOME;
    }
    get wholeGenomeType () {
        return WHOLE_GENOME;
    }
    get positiveStrand () {
        return POSITIVE_STRAND;
    }
    get negativeStrand () {
        return NEGATIVE_STRAND;
    }
    get positive () {
        return POSITIVE;
    }
    get negative () {
        return NEGATIVE;
    }

    static instance (
        appLayout,
        dispatcher,
        projectContext,
        motifsDataService
    ) {
        return new ngbMotifsPanelService(
            appLayout,
            dispatcher,
            projectContext,
            motifsDataService
        );
    }

    constructor(
        appLayout,
        dispatcher,
        projectContext,
        motifsDataService
    ) {
        Object.assign(this, {
            appLayout,
            dispatcher,
            projectContext,
            motifsDataService
        });
        this.dispatcher.on('reference:change', this.panelCloseMotifsPanel.bind(this));
    }

    panelAddMotifsPanel (params) {
        this.resetResultsData();
        this.setMotifsPanel(params);
        const layoutChange = this.appLayout.Panels.motifs;
        if (!layoutChange.displayed) {
            layoutChange.displayed = true;
            this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
        }
    }

    panelCloseMotifsPanel () {
        this.resetData();
        const layoutChange = this.appLayout.Panels.motifs;
        layoutChange.displayed = false;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    async setMotifsPanel (params) {
        const searchParams = this.setSearchMotifsParams(params);
        await this.searchMotif(searchParams);
        const firstHit = (this.searchMotifResults || [])[0];
        if (firstHit) {
            this.dispatcher.emitSimpleEvent('motifs:add:tracks', firstHit);
        }
    }

    setSearchMotifsParams (params) {
        const currentChromosomeId = this.projectContext.currentChromosome.id;
        const searchParams = {
            currentChromosomeId,
            name: params.title,
            motif: params.pattern,
            'search type': params.inReference ?
                this.referenceType : this.chromosomeType,
        };
        this._searchMotifsParams.push(searchParams);
        return searchParams;
    }

    async searchMotif (params) {
        this.requestNumber++;
        this.isSearchInProgress = true;
        this.isShowParamsTable = false;
        this.motifsResultsTitle = params.name || params.motif;
        const request = this.setSearchMotifRequest(params);
        await this.showSearchMotifResults(request);
        this.searchRequestsHistory.push(request);
    }

    setSearchMotifRequest(params) {
        const referenceType = params['search type'] === this.referenceType;
        const currentParams = {
            referenceId: this.projectContext.reference.id,
            motif: params.motif,
            searchType: referenceType ?
                this.wholeGenomeType : this.chromosomeType,
            pageSize: this.pageSize,
        };
        this.setCurrentParams(currentParams);
        const chromosomeId = params.currentChromosomeId || this.projectContext.currentChromosome.id;
        return referenceType ?
            {...currentParams} : {chromosomeId, ...currentParams};
    }

    setCurrentParams (params) {
        if (JSON.stringify(params) === '{}') {
            this._currentParams = {};
        } else {
            const name = this.motifsResultsTitle;
            this._currentParams = {
                name,
                ...params
            };
        }
    }

    async showSearchMotifResults(request) {
        await this.getSearchMotifsResults(request)
            .then(success => {
                const result = success ? this.searchMotifResults : [];
                return this.dispatcher.emitSimpleEvent('motifs:show:results', result);
            });
    }

    getSearchMotifsResults(request) {
        return new Promise((resolve) => {
            this.motifsDataService.getSearchMotifsResults(request)
                .then(response => {
                    this.isSearchInProgress = false;
                    this.isSearchFailure = false;
                    this.errorMessageList = null;
                    this.setSearchMotifResults(response.result);
                    this.searchStopOn = {
                        startPosition: response.position !== undefined ?
                            response.position : null,
                        chromosomeId: response.chromosomeId || null
                    };
                    resolve(true);
                })
                .catch((error) => {
                    this.isSearchInProgress = false;
                    this.isSearchFailure = true;
                    this.errorMessageList = [error.message];
                    resolve(false);
                });
        });
    }

    setSearchMotifResults (result) {
        this.searchMotifResults = [...(result || []).map(item => {
            const strand = this.setStrand(item.strand);
            return {
                reference: this.projectContext.reference.id,
                chromosome: item.contig,
                start: item.start,
                end: item.end,
                strand,
                gene: item.geneNames
            };
        })];
    }

    setStrand (strand) {
        if (strand === this.positive) {
            return this.positiveStrand;
        }
        if (strand === this.negative) {
            return this.negativeStrand;
        }
    }

    getStrand (strand) {
        if (strand === this.positiveStrand) {
            return this.positive;
        }
        if (strand === this.negativeStrand) {
            return this.negative;
        }
    }

    resetResultsData () {
        this._currentParams = {};
        this.motifsResultsTitle = '';
        this.searchRequestsHistory = [];
        this.searchMotifResults = [];
    }

    backToParamsTable () {
        this.resetResultsData();
        this.isShowParamsTable = true;
        this.dispatcher.emitSimpleEvent('motifs:show:params');
    }

    resetData () {
        this.resetResultsData();
        this._searchMotifsParams = [];
        this.searchStopOn = {};
    }
}
