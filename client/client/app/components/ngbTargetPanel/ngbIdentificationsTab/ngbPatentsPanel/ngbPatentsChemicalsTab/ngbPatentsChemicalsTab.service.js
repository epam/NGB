const SEARCH_BY_OPTIONS = {
    name: 'name',
    structure: 'structure',
};

const SEARCH_BY_NAMES = {
    [SEARCH_BY_OPTIONS.name]: 'drug name',
    [SEARCH_BY_OPTIONS.structure]: 'structure identifier (CID, SMILES, InChl)',
};

const PAGE_SIZE = 20;

export default class ngbPatentsChemicalsTabService {


    get searchByOptions() {
        return SEARCH_BY_OPTIONS;
    }
    get searchByNames() {
        return SEARCH_BY_NAMES;
    }
    get pageSize() {
        return PAGE_SIZE;
    }

    _loadingDrugs = false;
    _failedDrugs = false;
    _errorDrugsMessage = null;
    drugs = [];
    _selectedDrug;
    _searchBy = this.searchByOptions.name;
    _searchStructure = 'test';
    _originalStructure = '';
    requestedModel = null;

    _failedResult = false;
    _errorMessageList = null;
    _loadingData = false;
    _emptyResults = false;

    _tableResults = null;
    _totalPages = 0;
    _currentPage = 1;
    _sortInfo = null;

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
    get searchStructure() {
        return this._searchStructure;
    }
    set searchStructure(value) {
        this._searchStructure = value;
    }
    get originalStructure() {
        return this._originalStructure;
    }
    set originalStructure(value) {
        this._originalStructure = value;
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

    get isSearchByDrugName() {
        return this.searchBy === this.searchByOptions.name;
    }
    get isSearchByDrugStructure() {
        return this.searchBy === this.searchByOptions.structure;
    }

    get drugModelChanged() {
        if (this.isSearchByDrugName) {
            const {searchBy, drugName} = this.requestedModel;
            if (searchBy === this.searchByOptions.name && drugName) {
                return drugName !== this.selectedDrug;
            }
            return true;
        }
        return false;
    }

    get structureModelChanged() {
        if (this.isSearchByDrugStructure && !this.isStructureEmpty) {
            const {searchBy, drugName, structure} = this.requestedModel;
            if (searchBy === this.searchByOptions.structure && drugName && structure) {
                return drugName !== this.selectedDrug || this.searchStructure !== structure;
            }
            return true;
        }
        return false;
    }

    get requestModelChanged() {
        return !this.requestedModel || this.drugModelChanged || this.structureModelChanged;
    }

    get isStructureEmpty() {
        return !this.searchStructure || !this.searchStructure.length
    }

    get searchDisabled() {
        console.log(this.isSearchByDrugStructure && this.isStructureEmpty, this.requestModelChanged)
        return this.loadingDrugs || this.loadingData ||
            (this.isSearchByDrugStructure && this.isStructureEmpty)
            || !this.requestModelChanged;
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbPatentsChemicalsTabService(dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbTargetPanelService, targetDataService});
        dispatcher.on('target:identification:reset', this.resetData.bind(this));
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

    async searchPatents() {
        const searchBy = this.searchBy;
        const drugName = this.selectedDrug;
        const structure = this.searchStructure;
        const originalStructure = this.originalStructure;
        this.currentPage = 1;
        const success = await this.getTableResults();
        if (success) {
            if (this.isSearchByDrugName) {
                this.requestedModel = { searchBy, drugName };
            }
            if (this.isSearchByDrugStructure) {
                this.requestedModel = { searchBy, drugName, structure, originalStructure };
            }
        } else {
            this.requestedModel = null;
        }
        this.dispatcher.emit('target:identification:patents:drug:results:updated');
        return;
    }

    getRequest() {
        const request = {
            page: this.currentPage,
            pageSize: this.pageSize
        };
        if (this.isSearchByDrugName) {
            request.name = this.selectedDrug;
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
        if (this.isSearchByDrugName) {
            return Promise.resolve(this.targetDataService.searchPatentsByDrug(request));
        }
        if (this.isSearchByDrugStructure) {
            return Promise.resolve(this.targetDataService.searchPatentsByStructure(request));
        }
    }

    setTableResults(data) {
        if (data.items) {
            if (this.isSearchByDrugName) {
                this._tableResults = [];
            }
            if (this.isSearchByDrugStructure) {
                this._tableResults = [];
            }
        } else {
            this._tableResults = [];
        }
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
        this._loadingDrugs = false;
        this.drugs = [];
        this._selectedDrug;
        this._searchBy = this.searchByOptions.name;
        this._searchStructure;
        this.requestedModel = null;
    }
}
