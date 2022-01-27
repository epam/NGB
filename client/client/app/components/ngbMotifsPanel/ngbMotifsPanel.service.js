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
    _searchMotifFilter = null;
    _blockFilterResults = null;

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

    get searchMotifFilter () {
        return this._searchMotifFilter;
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
        this.dispatcher.emitSimpleEvent('motifs:add:tracks');
    }

    setSearchMotifsParams (params) {
        const {id, name} = this.projectContext.currentChromosome;
        const searchParams = {
            currentChromosomeId: id,
            name: params.title,
            motif: params.pattern,
            'search type': params.inReference ?
                this.referenceType : `${this.chromosomeType} [${name}]`,
        };
        this._searchMotifsParams.push(searchParams);
        return searchParams;
    }

    async searchMotif (params) {
        this.requestNumber++;
        this.isSearchInProgress = true;
        this.isShowParamsTable = false;
        this.resetResultsData();
        this.motifsResultsTitle = params.name || params.motif;
        const request = this.setSearchMotifRequest(params);
        await this.showSearchMotifResults(request);
        this.searchRequestsHistory.push(request);
    }

    setSearchMotifRequest(params) {
        const searchType = params['search type'].split(' ')[0];
        const referenceType = searchType === this.referenceType;
        const currentParams = {
            referenceId: this.projectContext.reference.id,
            motif: params.motif,
            searchType: referenceType ?
                this.wholeGenomeType : this.chromosomeType,
            pageSize: this.pageSize,
        };
        this.setCurrentParams(currentParams);
        const chromosomeId = params.currentChromosomeId || this.projectContext.currentChromosome.id;
        this.initializeFilters(chromosomeId);
        const request = referenceType ?
            {...currentParams} : {chromosomeId, ...currentParams};
        request.filter = this.getRequestFilter();
        return request;
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
            .then(result => {
                this.dispatcher.emitSimpleEvent('motifs:show:results');
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
                    resolve(response.result);
                })
                .catch((error) => {
                    this.isSearchInProgress = false;
                    this.isSearchFailure = true;
                    this.errorMessageList = [error.message];
                    resolve([]);
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
                gene: (item.geneNames || []).join(', ')
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

    canScheduleFilterResults () {
        return !this._blockFilterResults;
    }

    scheduleFilterResults() {
        if (this._blockFilterResults) {
            return;
        }
        this.dispatcher.emit('motifs:refresh:results');
    }

    initializeFilters (chromosomeId) {
        if (this.currentParams.searchType === this.chromosomeType && chromosomeId) {
            const chromosomeName = this.projectContext.chromosomes
                .filter(chr => chr.id === chromosomeId)
                .map(chr => chr.name)[0];
            this._searchMotifFilter = {
                chromosome: [chromosomeName],
                strand: '',
                gene: []
            };
        } else {
            this._searchMotifFilter = null;
        }
        this.dispatcher.emitSimpleEvent('initialize:motif:filters');
    }
        

    setFilter (key, value) {
        const filter = {...this.searchMotifFilter};
        filter[key] = [...value];
        
        this._searchMotifFilter = filter;
    }

    getRequestFilter () {
        if (!this.searchMotifFilter) {
            return;
        }
        const {chromosome, gene, strand} = this.searchMotifFilter;
        const filter = {};
        if (this.currentParams.searchType !== this.chromosomeType && chromosome) {
            filter.chromosomeIds = this.projectContext.chromosomes
                .filter(chr => (chromosome || []).indexOf(chr.name) >= 0)
                .map(chr => chr.id);
        }
        if (gene && gene.length) {
            filter.geneNames = [...gene];
        }
        if (strand) {
            filter.strand = this.getStrand(strand[0]);
        }
        return filter;
    }

    async filterResults () {
        const currentParams = {...this.currentParams};
        delete currentParams.name;
        this.searchRequestsHistory = [];
        const wholeGenomeType = currentParams.searchType === this.wholeGenomeType;
        const chromosomeId = this.projectContext.currentChromosome.id;
        const request = wholeGenomeType ?
            {...currentParams} : {chromosomeId, ...currentParams};
        request.filter = this.getRequestFilter();
        await this.getSearchMotifsResults(request)
            .then(result => {
                this.setSearchMotifResults(result);
            });
        this.searchRequestsHistory.push(request);
    }

    resetResultsData () {
        this._currentParams = {};
        this.motifsResultsTitle = null;
        this.searchRequestsHistory = [];
        this.searchMotifResults = null;
        this._searchMotifFilter = null;
    }

    backToParamsTable () {
        this.isShowParamsTable = true;
        this.resetResultsData();
        this.dispatcher.emitSimpleEvent('motifs:show:params');
    }

    resetData () {
        this.resetResultsData();
        this._searchMotifsParams = [];
        this.searchStopOn = {};
    }
}
