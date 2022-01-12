const REFERENCE = 'REFERENCE';
const CHROMOSOME = 'CHROMOSOME';
const POSITIVE = 'POSITIVE';
const NEGATIVE = 'NEGATIVE';
const POSITIVE_STRAND = '+';
const NEGATIVE_STRAND = '-';

export default class ngbMotifsPanelService {

    _isSearchInProgress = false;
    _isSearchFailure = false;
    _errorMessageList = null;
    _isShowParamsTable = true;

    requestNumber = 0;

    _searchMotifsParams = [];
    _searchMotifResults = [];
    searchStopOn = {};

    get referenceType () {
        return REFERENCE;
    }

    get chromosomeType () {
        return CHROMOSOME;
    }

    get positive () {
        return POSITIVE;
    }

    get negative () {
        return NEGATIVE;
    }

    get positiveStrand () {
        return POSITIVE_STRAND;
    }

    get negativeStrand () {
        return NEGATIVE_STRAND;
    }

    static instance (appLayout, dispatcher, projectContext, motifsDataService) {
        return new ngbMotifsPanelService(appLayout, dispatcher, projectContext, motifsDataService);
    }

    constructor(appLayout, dispatcher, projectContext, motifsDataService) {
        Object.assign(this, {appLayout, dispatcher, projectContext, motifsDataService});
        this.dispatcher.on('reference:change', ::this.panelCloseMotifsPanel);
    }

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

    set searchMotifsParams (params) {
        const currentChromosomeId = this.projectContext.currentChromosome.id;
        this._searchMotifsParams.push({
            currentChromosomeId,
            name: params.title,
            motif: params.pattern,
            'search type': params.inReference ?
                this.referenceType : this.chromosomeType,
        });
    }

    get searchMotifResults () {
        return this._searchMotifResults;
    }

    set searchMotifResults (result) {
        this._searchMotifResults = [...(result || []).map(item => {
            const strand = this.setStrand(item.strand);
            return {
                reference: this.projectContext.reference.id,
                chromosome: item.contig,
                start: item.start,
                end: item.end,
                strand
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

    panelAddMotifsPanel () {
        const layoutChange = this.appLayout.Panels.motifs;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    panelCloseMotifsPanel () {
        this.resetData();
        const layoutChange = this.appLayout.Panels.motifs;
        layoutChange.displayed = false;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    searchMotif (params) {
        this.requestNumber++;
        this.searchMotifsParams = params;
        this.dispatcher.emitSimpleEvent('motifs:search:change');
        this.panelAddMotifsPanel();
        this.isShowParamsTable = false;
        this.dispatcher.emitSimpleEvent('motifs:show:results', this.searchMotifsParams[0]);
    }

    searchMotifRequest (request) {
        this.isSearchInProgress = true;
        return new Promise((resolve) => {
            this.motifsDataService.getSearchMotifsResults(request)
                .then(response => {
                    this.isSearchInProgress = false;
                    this.isSearchFailure = false;
                    this.searchMotifResults = response.result;
                    this.searchStopOn = {
                        startPosition: response.position !== undefined ? response.position : null,
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

    resetData () {
        this._searchMotifsParams = [];
        this.searchStopOn = {};
    }
}
