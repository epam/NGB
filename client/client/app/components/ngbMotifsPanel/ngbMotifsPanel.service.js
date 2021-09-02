function generateForChromosome (motif, context, reference) {
    const chr = context.currentChromosome.name;
    const size = context.currentChromosome.size;
    const length = motif.length;
    const data = [];

    for (let i = 0; i < 50; i++) {
        const strandBoolean = [true, false][ Math.floor( Math.random() * 2 ) ];
        const randomNumber = Math.floor(Math.random() * size) + 1;
        const start = strandBoolean ? randomNumber : (randomNumber - length);
        const end = strandBoolean ? (randomNumber + length) : randomNumber;
        data.push({
            motif,
            reference,
            chromosome : chr,
            start,
            end,
            strand: strandBoolean ? 'POSITIVE' : 'NEGATIVE'
        });
    }
    return data;
}

function generateForReference (motif, context, reference) {
    const data = [];

    for (let i = 0; i < 100; i++) {
        const randomChr = Math.floor(Math.random() * context.chromosomes.length);
        const chr = context.chromosomes[randomChr].name;
        const size = context.chromosomes[randomChr].size;
        const length = motif.length;

        const strandBoolean = [true, false][ Math.floor( Math.random() * 2 ) ];
        const randomNumber = Math.floor(Math.random() * size) + 1;
        const start = strandBoolean ? randomNumber : (randomNumber - length);
        const end = strandBoolean ? (randomNumber + length) : randomNumber;
        data.push({
            motif,
            reference: reference.name,
            chromosome : chr,
            start,
            end,
            strand: strandBoolean ? 'POSITIVE' : 'NEGATIVE'
        });
    }
    return data;
}

const ROW_HEIGHT = 35;

export default class ngbMotifsPanelService {

    reference = null;
    requestNumber = 0;
    _isSearchInProgress = false;
    _isSearchFailure = false;
    _errorMessageList = null;
    _searchMotifsParams = [];
    _searchMotifResults = [];
    searchStopOnPosition = 0;
    searchStopOnChromosome;
    currentParams = {};

    static instance(dispatcher, appLayout, projectContext, motifsDataService) {
        return new ngbMotifsPanelService(dispatcher, appLayout, projectContext, motifsDataService);
    }

    constructor(dispatcher, appLayout, projectContext, motifsDataService) {
        Object.assign(this, {dispatcher, appLayout, projectContext, motifsDataService});
        this.dispatcher.on('motifs:search:reset', ::this.resetData);
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

    get isSearchResults () {
        return Boolean(this.searchMotifsParams.length);
    }

    get searchMotifsParams () {
        return [...this._searchMotifsParams].reverse();
    }

    set searchMotifsParams (params) {
        const requestNumber = this.requestNumber;
        this._searchMotifsParams.push({
            requestNumber,
            'search type': params.chromosomeOnly  ? 'CHROMOSOME' : 'WHOLE_GENOME',
            motif: params.pattern,
            name: params.title,
        });
    }

    get searchMotifResults () {
        return this._searchMotifResults;
    }

    set searchMotifResults (result) {
        this._searchMotifResults = result.map(item => {
            return {
                reference: this.reference.id,
                chromosome: item.contig,
                start: item.start,
                end: item.end,
                strand: item.strand
            };
        });
    }

    get rowHeight () {
        return ROW_HEIGHT;
    }

    get pageSize () {
        return Math.floor(window.innerHeight / this.rowHeight);
    }

    panelAddMotifsPanel () {
        const layoutChange = this.appLayout.Panels.motifs;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    searchMotif(params) {
        this.requestNumber++;
        this.searchMotifsParams = params;
        this.searchStopOnChromosome = params.chromosomeOnly ?
            this.projectContext.currentChromosome.id :
            this.projectContext.chromosomes[0].id;
        this.reference = this.projectContext.reference;
        this.dispatcher.emitSimpleEvent('motifs:search:change');
        this.panelAddMotifsPanel();
    }

    searchMotifRequest (request) {
        this.isSearchInProgress = true;
        return new Promise((resolve) => {
            this.motifsDataService.getSearchMotifsResults(request)
                .then(response => {
                    this.isSearchInProgress = false;
                    this.isSearchFailure = false;
                    this.searchMotifResults = response.result;
                    this.searchStopOnPosition = response.position;
                    this.searchStopOnChromosome = response.chromosomeId;
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

    async resultsTableData (row) {
        const chromosomeOnly = row['search type'] === 'CHROMOSOME';
        this.currentParams = {
            referenceId: this.projectContext.reference.id,
            motif: row.motif,
            searchType: row['search type'],
            pageSize: this.pageSize,
        };
        const request = chromosomeOnly ?
            {chromosomeId: this.projectContext.currentChromosome.id, ...this.currentParams} :
            {...this.currentParams};
        return this.searchMotifRequest(request);
    }

    async getNextResults () {
        const params = this.currentParams;
        const startPosition = this.searchStopOnPosition;
        const chromosomeId = this.searchStopOnChromosome;
        const request = {chromosomeId, startPosition, ...params};
        return this.searchMotifRequest(request);
    }

    async getPreviousResults () {
        const params = this.currentParams;
        const startPosition = this.searchStopOnPosition;
        const chromosomeId = this.searchStopOnChromosome;
        const request = {chromosomeId, startPosition, ...params};
        return this.searchMotifRequest(request);
    }

    resetData () {
        this.currentParams = {};
        this._searchMotifsParams = [];
    }
}
