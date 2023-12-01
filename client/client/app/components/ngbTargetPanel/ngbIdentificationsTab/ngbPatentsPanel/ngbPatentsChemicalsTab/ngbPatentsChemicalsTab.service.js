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
    _failedDrugs = false;
    _errorDrugsMessage = null;
    drugs = [];
    _selectedDrug;
    _searchBy = this.searchByOptions.name;

    get loadingDrugs() {
        return this._loadingDrugs;
    }
    get failedDrugs() {
        return this._failedDrugs;
    }
    get errorDrugsMessage() {
        return this._errorDrugsMessage;
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

    static instance (dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbPatentsChemicalsTabService(dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbTargetPanelService, targetDataService});
        this.setDrugs();
    }

    get geneIds() {
        return [...this.ngbTargetPanelService.allGenes.map(i => i.geneId)];
    }

    async setDrugs() {
        this._loadingDrugs = true;
        this.drugs = await this.getDrugs();
        this.selectedDrug = this.drugs[0];
        this.dispatcher.emit('target:identification:patents:sequences:drugs:updated');
    }

    getDrugs() {
        if (!this.geneIds) {
            return new Promise(resolve => {
                this._loadingDrugs = false;
                resolve([]);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getDrugs(this.geneIds)
                .then(data => {
                    this._failedDrugs = false;
                    this._errorDrugsMessage = null;
                    this._loadingDrugs = false;
                    resolve(data);
                })
                .catch(err => {
                    this._failedDrugs = true;
                    this._errorDrugsMessage = [err.message];
                    this._loadingDrugs = false;
                    resolve([]);
                });
        });
    }
}
