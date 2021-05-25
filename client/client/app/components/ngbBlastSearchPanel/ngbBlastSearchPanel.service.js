const BLAST_STATES = {
    HISTORY: 'HISTORY',
    RESULT: 'RESULT',
    SEARCH: 'SEARCH'
};

export default class ngbBlastSearchService {
    static instance(dispatcher, projectContext, bamDataService, projectDataService) {
        return new ngbBlastSearchService(dispatcher, projectContext, bamDataService, projectDataService);
    }

    _detailedRead = null;
    bamDataService;
    _totalPagesCountHistory = 0;
    _currentResultId = null;
    _currentSearchId = null;

    get totalPagesCountHistory() {
        return this._totalPagesCountHistory;
    }

    set totalPagesCountHistory(value) {
        this._totalPagesCountHistory = value;
    }

    get blastStates() {
        return BLAST_STATES;
    }

    constructor(dispatcher, projectContext, bamDataService, projectDataService) {
        Object.assign(this, {dispatcher, projectContext, bamDataService, projectDataService});
    }

    generateSpeciesList() {
        return [
            this.projectContext.reference,
            {id: '1eds52', name: 'GRCh38'},
            {id: '1adc47', name: 'Bacteria Escherichia coli'},
            {id: '4etr89', name: 'Clostridium botulinum'},
        ];
    }

    get blastRequest() {
        return JSON.parse(localStorage.getItem('blastSearchRequest')) || null;
    }

    get readSequence() {
        return this._detailedRead && this._detailedRead.sequence ? this._detailedRead.sequence : null;
    }

    async getDetailedRead(payload) {
        const read = await this.bamDataService.loadRead(payload);

        this._detailedRead = read ? read : null;

        return this._detailedRead;
    }

    set currentSearchId(currentSearchId) {
        this._currentSearchId = currentSearchId;
    }

    get currentResultId() {
        return this._currentResultId;
    }

    set currentResultId(currentResultId) {
        this._currentResultId = currentResultId;
    }

    get currentSearch() {
        return {};
    }

    async getCurrentSearchResult() {
        let data = {};
        if (this.currentResultId) {
            data = await this.projectDataService.getBlastSearch(this.currentResultId);
        }
        return data;
    }
}
