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

    static instance (dispatcher) {
        return new ngbPatentsSequencesTabService(dispatcher);
    }

    constructor(dispatcher) {
        Object.assign(this, {dispatcher});
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
}
