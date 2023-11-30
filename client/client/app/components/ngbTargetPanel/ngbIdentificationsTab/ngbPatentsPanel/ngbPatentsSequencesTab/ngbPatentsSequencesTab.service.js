const SEARCH_BY_OPTIONS = {
    name: 'name',
    sequence: 'sequence',
};

const SEARCH_BY_NAMES = {
    [SEARCH_BY_OPTIONS.name]: 'protein name',
    [SEARCH_BY_OPTIONS.sequence]: 'amino acid sequence',
};

export default class ngbPatentsSequencesTabService {

    get searchByOptions() {
        return SEARCH_BY_OPTIONS;
    }
    get searchByNames() {
        return SEARCH_BY_NAMES;
    }

    _loadingProteins = false;
    proteins = [];
    _selectedProtein;
    _searchBy = this.searchByOptions.name;
    _searchSequence = 'ABFKGGBDKFHRHGBG';
    requestedModel = null;

    _failedResult = false;
    _errorMessageList = null;
    _loadingData = false;

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

    static instance (dispatcher, targetDataService) {
        return new ngbPatentsSequencesTabService(dispatcher, targetDataService);
    }

    constructor(dispatcher, targetDataService) {
        Object.assign(this, {dispatcher, targetDataService});
        dispatcher.on('target:identification:sequences:updated', this.setProteins.bind(this));
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

    searchPatents() {
        if (this.isSearchByProteinName) {
            return this.searchPatentsByProtein();
        }
        if (this.isSearchByProteinSequence) {
            return this.serachPatentsBySequence();
        }
    }

    searchPatentsByProtein() {
        const searchBy = this.searchBy;
        const proteinId = this.selectedProtein.id;
        return new Promise(resolve => {
            this.targetDataService.searchPatentsByProtein()
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this.requestedModel = { searchBy, proteinId }
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this.requestedModel = null;
                    this._loadingData = false;
                    resolve(true);
                });
        });
    }

    serachPatentsBySequence() {
        const searchBy = this.searchBy;
        const proteinId = this.selectedProtein.id;
        const sequence = this.searchSequence;
        return new Promise(resolve => {
            this.targetDataService.serachPatentsBySequence()
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this.requestedModel = { searchBy, proteinId, sequence }
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this.requestedModel = null;
                    this._loadingData = false;
                    resolve(true);
                });
        });
    }
}
