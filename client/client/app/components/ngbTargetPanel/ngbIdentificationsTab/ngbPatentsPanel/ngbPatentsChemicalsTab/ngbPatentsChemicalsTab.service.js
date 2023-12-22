const SEARCH_BY_OPTIONS = {
    name: 'name',
    structure: 'structure',
};

const SEARCH_BY_NAMES = {
    [SEARCH_BY_OPTIONS.name]: 'drug name',
    [SEARCH_BY_OPTIONS.structure]: 'structure identifier (CID, SMILES, InChl)',
};

const PAGE_SIZE = 20;

const DRUG_COLUMNS = ['CID', 'Name', 'Molecular formula', 'IUPAC name'];
const STRUCTURE_COLUMNS = ['CID', 'Name', 'Molecular formula', 'IUPAC name', 'Patent'];

const HEADER_TEXT = {
    [SEARCH_BY_OPTIONS.name]: 'Patented chemicals containing the name of the specified drug - ',
    [SEARCH_BY_OPTIONS.structure]: 'Patented chemicals identical/similar to the specified identifier - ',
};

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
    get drugColumns() {
        return DRUG_COLUMNS;
    }
    get structureColumns() {
        return STRUCTURE_COLUMNS;
    }
    get headerText() {
        return HEADER_TEXT;
    }

    _loadingDrugs = false;
    _failedDrugs = false;
    _errorDrugsMessage = null;
    drugs = [];
    _selectedDrug;
    _searchBy = this.searchByOptions.name;
    _searchStructure = '';
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

    _loadingIdentifier = false;
    _failedIdentifier = false;
    _errorIdentifier = null;

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
            if (searchBy === this.searchByOptions.structure && (drugName && this.selectedDrug) && structure) {
                return drugName !== this.selectedDrug || this.searchStructure !== structure;
            }
            if (searchBy === this.searchByOptions.structure && structure) {
                return this.searchStructure !== structure;
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
        return this.loadingDrugs || this.loadingData ||
            (this.isSearchByDrugStructure && this.isStructureEmpty) ||
            (this.isSearchByDrugName && !this.selectedDrug) ||
            !this.requestModelChanged;
    }

    get loadingIdentifier() {
        return this._loadingIdentifier;
    }
    set loadingIdentifier(value) {
        this._loadingIdentifier = value;
    }
    get failedIdentifier() {
        return this._failedIdentifier;
    }
    set failedIdentifier(value) {
        this._failedIdentifier = value;
    }
    get errorIdentifier() {
        return this._errorIdentifier;
    }
    set errorIdentifier(value) {
        this._errorIdentifier = value;
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbPatentsChemicalsTabService(dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbTargetPanelService, targetDataService});
        dispatcher.on('target:identification:reset', this.targetChanged.bind(this));
        this.setDrugs();
    }

    targetChanged() {
        this.resetData();
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
        this.requestedModel = { searchBy: this.searchBy };
        this.currentPage = 1;
        const drugName = this.selectedDrug;
        if (drugName) {
            this.requestedModel.drugName = drugName;
        }
        const structure = this.searchStructure;
        if (this.searchBy === this.searchByOptions.structure && structure) {
            this.requestedModel.structure = structure;
            this.requestedModel.originalStructure = this.originalStructure;
        }
        this.currentPage = 1;
        const success = await this.getTableResults();
        if (!success) {
            this.requestedModel = null;
        }
        this.dispatcher.emit('target:identification:patents:drug:pagination:updated');
        return;
    }

    getRequest() {
        let request;
        const {searchBy} = this.requestedModel;
        if (searchBy === this.searchByOptions.name) {
            request = {
                page: this.currentPage,
                pageSize: this.pageSize,
                name: this.selectedDrug,
            };
        }
        if (searchBy === this.searchByOptions.structure) {
            request = this.searchStructure;
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
        const {searchBy} = this.requestedModel;
        if (searchBy === this.searchByOptions.name) {
            return Promise.resolve(this.targetDataService.searchPatentsByDrug(request));
        }
        if (searchBy === this.searchByOptions.structure) {
            return Promise.resolve(this.targetDataService.searchPatentsByStructure(request));
        }
    }

    setTableResults(data) {
        if (data.items) {
            const {searchBy} = this.requestedModel;
            if (searchBy === this.searchByOptions.name) {
                this._tableResults = data.items.map(item => {
                    const cid = { name: item.id };
                    if (item.url) {
                        cid.url = item.url;
                    }
                    return {
                        'CID': cid,
                        'Name': item.name,
                        'Molecular formula': item.molecularFormula,
                        'IUPAC name': item.iupacName,
                    };
                });
            }
            if (searchBy === this.searchByOptions.structure) {
                this._tableResults = data.items.map(item => {
                    const cid = { name: item.id };
                    if (item.url) {
                        cid.url = item.url;
                    }
                    return {
                        'CID': cid,
                        'Name': item.name,
                        'Molecular formula': item.molecularFormula,
                        'IUPAC name': item.iupacName,
                        'Patent': item.hasPatent
                    };
                });
            }
        } else {
            this._tableResults = [];
        }
    }

    getColumnList() {
        const {name, structure} = this.searchByOptions;
        if (this.searchBy === name) {
            return this.drugColumns;
        }
        if (this.searchBy === structure) {
            return this.structureColumns;
        }
    }

    async getIdentifier() {
        if (!this.selectedDrug) {
            return new Promise(resolve => {
                this._loadingIdentifier = false;
                resolve(false);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getCompound(this.selectedDrug)
                .then(data => {
                    this._failedIdentifier = false;
                    this._errorIdentifier = null;
                    this._loadingIdentifier = false;
                    resolve(data);
                })
                .catch(err => {
                    this._failedIdentifier = true;
                    this._errorIdentifier = [err.message];
                    this._loadingIdentifier = false;
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
        this._loadingDrugs = false;
        this.drugs = [];
        this._selectedDrug;
        this._searchBy = this.searchByOptions.name;
        this._searchStructure;
        this.requestedModel = null;
        this._failedIdentifier = false;
        this._errorIdentifier = null;
        this._loadingIdentifier = false;
    }
}
