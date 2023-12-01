const SEARCH_BY_OPTIONS = {
    name: 'name',
    sequence: 'sequence',
};

const SEARCH_BY_NAMES = {
    [SEARCH_BY_OPTIONS.name]: 'protein name',
    [SEARCH_BY_OPTIONS.sequence]: 'amino acid sequence',
};

const HEADER_TEXT = {
    [SEARCH_BY_OPTIONS.name]: 'Patented sequences containing the name of the specified protein - ',
    [SEARCH_BY_OPTIONS.sequence]: 'Patented sequences identical/similar to the specified query - ',
};

const PAGE_SIZE = 20;

const PROTEIN_COLUMNS = [{
        name: 'protein',
        displayName: 'Protein'
    }, {
        name: 'length',
        displayName: 'Length (aa)'
    }, {
        name: 'organism',
        displayName: 'Organism'
    }, {
        name: 'name',
        displayName: 'Protein name'
}];

const SEQUENCE_COLUMNS = [{
    name: 'protein',
    displayName: 'Protein'
}, {
    name: 'length',
    displayName: 'Length (aa)'
}, {
    name: 'organism',
    displayName: 'Organism'
}, {
    name: 'name',
    displayName: 'Protein name'
}, {
    name: 'query cover',
    displayName: 'Query cover'
}, {
    name: 'percent identity',
    displayName: 'Percent identity'
}];

const SEQUENCE_DB = 'PROTEIN';

export default class ngbPatentsSequencesTabService {

    get searchByOptions() {
        return SEARCH_BY_OPTIONS;
    }
    get searchByNames() {
        return SEARCH_BY_NAMES;
    }
    get pageSize() {
        return PAGE_SIZE;
    }
    get proteinColumns() {
        return PROTEIN_COLUMNS;
    }
    get sequenceColumns() {
        return SEQUENCE_COLUMNS;
    }
    get sequenceDB() {
        return SEQUENCE_DB;
    }
    get headerText() {
        return HEADER_TEXT;
    }

    _loadingProteins = false;
    proteins = [];
    _selectedProtein;
    _searchBy = this.searchByOptions.name;
    _searchSequence = '';
    _originalSequence = '';
    requestedModel = null;

    _failedResult = false;
    _errorMessageList = null;
    _loadingData = false;
    _emptyResults = false;

    _tableResults = null;
    _totalPages = 0;
    _currentPage = 1;
    _sortInfo = null;

    _loadingSequence = false;
    _failedSequence = false;
    _errorSequence = null;

    get loadingProteins() {
        return this._loadingProteins;
    }
    set loadingProteins(value) {
        this._loadingProteins = value;
    }
    get selectedProtein() {
        return this._selectedProtein;
    }
    set selectedProtein(value) {
        this._selectedProtein = value;
    }
    get searchBy() {
        return this._searchBy;
    }
    set searchBy(value) {
        this._searchBy = value;
    }
    get searchSequence() {
        return this._searchSequence;
    }
    set searchSequence(value) {
        this._searchSequence = value;
    }
    get originalSequence() {
        return this._originalSequence;
    }
    set originalSequence(value) {
        this._originalSequence = value;
    }
    get isSearchByProteinName() {
        return this.searchBy === this.searchByOptions.name;
    }
    get isSearchByProteinSequence() {
        return this.searchBy === this.searchByOptions.sequence;
    }

    get proteinModelChanged() {
        if (this.isSearchByProteinName) {
            const {searchBy, proteinId} = this.requestedModel;
            if (searchBy === this.searchByOptions.name && proteinId) {
                return proteinId !== this.selectedProtein.id;
            }
            return true;
        }
        return false;
    }

    get sequenceModelChanged() {
        if (this.isSearchByProteinSequence && !this.isSequenceEmpty) {
            const {searchBy, proteinId, sequence} = this.requestedModel;
            if (searchBy === this.searchByOptions.sequence && proteinId && sequence) {
                return proteinId !== this.selectedProtein.id || this.searchSequence !== sequence;
            }
            return true;
        }
        return false;
    }

    get requestModelChanged() {
        return !this.requestedModel || this.proteinModelChanged || this.sequenceModelChanged;
    }

    get isSequenceEmpty() {
        return !this.searchSequence || !this.searchSequence.length
    }

    get searchDisabled() {
        return this.loadingProteins || this.loadingData ||
            (this.isSearchByProteinSequence && this.isSequenceEmpty)
            || !this.requestModelChanged;
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
    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
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
    get tableResults() {
        return this._tableResults;
    }
    get sortInfo() {
        return this._sortInfo;
    }
    set sortInfo(value) {
        this._sortInfo = value;
    }

    get loadingSequence() {
        return this._loadingSequence;
    }
    set loadingSequence(value) {
        this._loadingSequence = value;
    }
    get failedSequence() {
        return this._failedSequence;
    }
    set failedSequence(value) {
        this._failedSequence = value;
    }
    get errorSequence() {
        return this._errorSequence;
    }
    set errorSequence(value) {
        this._errorSequence = value;
    }

    static instance (dispatcher, targetDataService) {
        return new ngbPatentsSequencesTabService(dispatcher, targetDataService);
    }

    constructor(dispatcher, targetDataService) {
        Object.assign(this, {dispatcher, targetDataService});
        dispatcher.on('target:identification:sequences:updated', this.setProteins.bind(this));
        dispatcher.on('target:identification:reset', this.resetData.bind(this));
    }

    setProteins(sequences) {
        this.loadingProteins = true;
        this.proteins = sequences.reduce((acc, curr) => {
            if (curr.sequences && curr.sequences.length) {
                for (let i = 0; i < curr.sequences.length; i++) {
                    const protein = curr.sequences[i].protein;
                    if (protein) {
                        acc.push(protein);
                    }
                }
            }
            return acc;
        }, []);
        this.selectedProtein = this.proteins[0];
        this.loadingProteins = false;
        this.dispatcher.emit('target:identification:patents:sequences:proteins:updated');
    }

    async searchPatents() {
        const searchBy = this.searchBy;
        const proteinId = this.selectedProtein.id;
        const proteinName = this.selectedProtein.name;
        const sequence = this.searchSequence;
        const originalSequence = this.originalSequence;
        this.currentPage = 1;
        const success = await this.getTableResults();
        if (success) {
            if (this.isSearchByProteinName) {
                this.requestedModel = { searchBy, proteinId, proteinName };
            }
            if (this.isSearchByProteinSequence) {
                this.requestedModel = { searchBy, proteinId, proteinName, sequence, originalSequence };
            }
        } else {
            this.requestedModel = null;
        }
        this.dispatcher.emit('target:identification:patents:protein:results:updated');
        return;
    }

    getRequest() {
        const request = {
            page: this.currentPage,
            pageSize: this.pageSize
        };
        if (this.isSearchByProteinName) {
            request.name = this.selectedProtein.name;
        }
        return request;
    }

    getTableResults() {
        const request = this.getRequest();
        if (!request) {
            return new Promise(resolve => {
                this.loadingData = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.searchPatentResults(request)
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._totalPages = Math.ceil(data.totalCount/this.pageSize);
                    this._emptyResults = data.totalCount === 0;
                    this.setTableResults(data);
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._totalPages = 0;
                    this._emptyResults = false;
                    this._tableResults = null;
                    this._loadingData = false;
                    resolve(false);
                });
        });
    }

    searchPatentResults(request) {
        if (this.isSearchByProteinName) {
            return Promise.resolve(this.targetDataService.searchPatentsByProtein(request));
        }
        if (this.isSearchByProteinSequence) {
            return Promise.resolve(this.targetDataService.searchPatentsBySequence(request));
        }
    }

    setTableResults(data) {
        if (data.items) {
            if (this.isSearchByProteinName) {
                this._tableResults = data.items.map(i => {
                    const protein = { name: i.id };
                    if (i.url) {
                        protein.url = i.url;
                    }
                    return {
                        protein,
                        length: i.length,
                        organism: i.organism,
                        name: i.name
                    };
                });
            }
            if (this.isSearchByProteinSequence) {
                this._tableResults = [];
            }
        } else {
            this._tableResults = [];
        }
    }

    getColumnList() {
        const {name, sequence} = this.searchByOptions;
        if (this.searchBy === name) {
            return this.proteinColumns;
        }
        if (this.searchBy === sequence) {
            return this.sequenceColumns;
        }
    }

    async getSequence() {
        const database = this.sequenceDB;
        const id = this.selectedProtein.id;
        return new Promise(resolve => {
            this.targetDataService.getSequence(database, id)
                .then(data => {
                    this._failedSequence = false;
                    this._errorSequence = null;
                    this._loadingSequence = false;
                    resolve(data);
                })
                .catch(err => {
                    this._failedSequence = true;
                    this._errorSequence = [err.message];
                    this._loadingSequence = false;
                    resolve(false);
                });
        });
    }

    resetTableResults() {
        this._tableResults = null;
        this._currentPage = 1;
        this._totalPages = 0;
        this._sortInfo = null;
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
    }

    resetData() {
        this.resetTableResults();
        this._loadingProteins = false;
        this.proteins = [];
        this._selectedProtein;
        this._searchBy = this.searchByOptions.name;
        this._searchSequence;
        this.requestedModel = null;
        this._loadingSequence = false;
        this._failedSequence = false;
        this._errorSequence = null;
    }
}
