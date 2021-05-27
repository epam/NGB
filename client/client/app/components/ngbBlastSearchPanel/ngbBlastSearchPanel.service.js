const BLAST_STATES = {
    HISTORY: 'HISTORY',
    RESULT: 'RESULT',
    SEARCH: 'SEARCH'
};

export default class ngbBlastSearchService {
    static instance(dispatcher, bamDataService, projectDataService) {
        return new ngbBlastSearchService(dispatcher, bamDataService, projectDataService);
    }

    _detailedRead = null;
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

    constructor(dispatcher, bamDataService, projectDataService) {
        Object.assign(this, {dispatcher, bamDataService, projectDataService});
    }

    async getOrganismList(term) {
        return await this.projectDataService.getOrganismList(term);
        // return [
        //     {id: '1eds52', name: 'GRCh38'},
        //     {id: '1adc47', name: 'Bacteria Escherichia coli'},
        //     {id: '4etr89', name: 'Clostridium botulinum'},
        // ];
    }

    async getBlastDBList() {
        return await this.projectDataService.getBlastDBList('NUCLEOTIDE');
        // return [1, 2, 3, 'Homo_sapiens.GRCh38'];
    }

    async _getDetailedRead() {
        const searchRequest = JSON.parse(localStorage.getItem('blastSearchRequest')) || null;
        let read = null;
        if (searchRequest) {
            read = await this.bamDataService.loadRead(searchRequest);
        }
        return read;
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

    async getCurrentSearch() {
        let data = {};
        if (this._currentSearchId) {
            data = this._formatServerToClient(await this.projectDataService.getBlastSearch(this._currentSearchId));
        } else {
            const newSearch = await this._getDetailedRead();
            if (newSearch) {
                data.sequence = newSearch.sequence;
            }
        }
        return data;
    }

    async getCurrentSearchResult() {
        let data = {};
        if (this.currentResultId) {
            data = await this.projectDataService.getBlastSearch(this.currentResultId);
        }
        return data;
    }

    createSearchRequest(searchRequest) {
        return this.projectDataService.createBlastSearch(this._formatClientToServer(searchRequest)).then(data => {
            if (data && data.id) {
                this.currentSearchId = data.id;
                localStorage.removeItem('blastSearchRequest');
            }
            this.currentSearchId = null;
        });
    }

    _formatServerToClient(search) {
        return {
            id: search.id,
            title: search.title,
            algorithm: search.algorithm,
            organisms: search.organisms,
            db: search.database,
            tool: search.executable,
            sequence: search.query
        };
    }

    _formatClientToServer(search) {
        const result = {
            title: search.title || '',
            algorithm: search.algorithm,
            organisms: search.organisms || [],
            database: search.db,
            executable: search.tool,
            query: search.sequence
        };
        if (search.id) {
            result.id = search.id;
        }
        return result;
    }
}
