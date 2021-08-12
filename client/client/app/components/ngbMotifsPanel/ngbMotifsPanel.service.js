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

let id = 0;
const ROW_HEIGHT = 35;

export default class ngbMotifsPanelService {

    _isSearchInProgress = false;
    _isSearchFailure = false;
    _errorMessageList = null;
    _firstLevelData = [];
    reference = null;
    _searchInCurrent = true;
    _data = [];

    static instance(dispatcher, appLayout, projectContext) {
        return new ngbMotifsPanelService(dispatcher, appLayout, projectContext);
    }

    constructor(dispatcher, appLayout, projectContext) {
        Object.assign(this, {dispatcher, appLayout, projectContext});
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

    get searchInCurrent () {
        return this._searchInCurrent;
    }

    set searchInCurrent (value) {
        this._searchInCurrent = value;
    }

    get isSearchResults () {
        return Boolean(this.firstLevelData.length);
    }

    get firstLevelData () {
        return [...this._firstLevelData].reverse();
    }

    set firstLevelData (request) {
        this._firstLevelData.push({
            id,
            motif: request.pattern,
            name: request.title,
            matches: this.getDataLength(id)
        });
    }

    get rowHeight () {
        return ROW_HEIGHT;
    }

    get pageSize () {
        return Math.floor(window.innerHeight / this.rowHeight);
    }

    allMatches (motif) {
        const result = this.searchInCurrent ?
            generateForChromosome(motif, this.projectContext, this.reference.name) :
            generateForReference(motif, this.projectContext, this.reference);
        this._data.push(result);
    }

    getData (index) {
        return this._data[index-1];
    }

    motifsRequest(request) {
        this.isSearchInProgress = true;
        if (request) {
            id++;
            this.reference = request.reference;
            this.searchInCurrent = request.inCurrent;
            this.allMatches(request.pattern);
            this.firstLevelData = request;
            this.isSearchInProgress = false;
            this.isSearchFailure = false;
            this.dispatcher.emitSimpleEvent('motifs:search:change');
            this.panelAddMotifsPanel();
        } else {
            this.isSearchInProgress = false;
            this.isSearchFailure = true;
        }
    }

    panelAddMotifsPanel () {
        const layoutChange = this.appLayout.Panels.motifs;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    getDataLength (row) {
        // find row in firstLevelData and get the matches
        return this.getData(row).length;
    }

    secondLevelData (row, page) {
        //find row in firstLevelData and the matches array
        const dataSlice = this.getData(row.id)
            .slice((page - 1) * this.pageSize, page * this.pageSize);
        return dataSlice;
    }

    resetData () {
        this._firstLevelData = [];
    }
}
