const SEARCH_BY_OPTIONS = {
    name: 'name',
    structure: 'structure',
};

const SEARCH_BY_NAMES = {
    [SEARCH_BY_OPTIONS.name]: 'drug name',
    [SEARCH_BY_OPTIONS.structure]: 'structure identifier (CID, SMILES, InChl)',
};

export default class ngbPatentsChemicalsTabService {


    get searchByOptions() {
        return SEARCH_BY_OPTIONS;
    }
    get searchByNames() {
        return SEARCH_BY_NAMES;
    }

    _loadingDrugs = false;
    drugs = [];
    _selectedDrug;
    _searchBy = this.searchByOptions.name;

    get loadingDrugs() {
        return this._loadingDrugs;
    }
    set loadingDrugs(value) {
        this._loadingDrugs = value;
    }
    get selectedDrug() {
        return this._selectedDrug;
    }
    set selectedDrug(value) {
        this._selectedDrug = value;
    }
    get searchBy() {
        return this._searchBy;
    }
    set searchBy(value) {
        this._searchBy = value;
    }

    static instance () {
        return new ngbPatentsChemicalsTabService();
    }

    constructor() {
        Object.assign(this, {});
        this.setDrugs();
    }

    setDrugs() {
        this.loadingDrugs = true;
        this.drugs = [{
            name: 'ALECTINIB'
        }, {
            name: 'BRIGATINIB'
        }, {
            name: 'CERITINIB'
        }];
        this.selectedDrug = this.drugs[0];
        this.loadingDrugs = false;
    }
}
