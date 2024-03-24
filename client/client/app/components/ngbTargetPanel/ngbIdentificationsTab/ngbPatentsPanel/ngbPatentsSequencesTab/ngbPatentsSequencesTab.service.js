const SEARCH_BY_OPTIONS = {
    name: 'name',
    sequence: 'sequence',
};

const SEARCH_BY_NAME = {
    name: 'name'
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
        name: 'title',
        displayName: 'Title'
    }, {
        name: 'journal',
        displayName: 'Journal'
    }, {
        name: 'name',
        displayName: 'Protein name'
    }, {
        name: 'length',
        displayName: 'Length (aa)'
    }, {
        name: 'organism',
        displayName: 'Organism'
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

const REFRESH_INTERVAL_SEC = 5;

const BLAST_SEARCH_STATE = {
    DONE: 'DONE',
    FAILURE: 'FAILURE',
    SEARCHING: 'SEARCHING',
    CANCELED: 'CANCELED'
};

export default class ngbPatentsSequencesTabService {

    get searchByOptions() {
        return this._disableProteinBlast ? SEARCH_BY_NAME : SEARCH_BY_OPTIONS;
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
    get refreshInterval() {
        return REFRESH_INTERVAL_SEC * 1000;
    }
    get blastSearchState() {
        return BLAST_SEARCH_STATE;
    }

    _disableProteinBlast;
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
    _allResults = null;

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
            if (searchBy === this.searchByOptions.sequence && (proteinId && this.selectedProtein) && sequence) {
                return proteinId !== this.selectedProtein.id || this.searchSequence !== sequence;
            }
            if (searchBy === this.searchByOptions.sequence && sequence) {
                return this.searchSequence !== sequence;
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
            (this.isSearchByProteinSequence && this.isSequenceEmpty) ||
            (this.isSearchByProteinName && !this.selectedProtein) ||
            !this.requestModelChanged;
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

    static instance (
        $interval,
        dispatcher,
        ngbTargetPanelService,
        targetDataService,
        projectDataService,
        utilsDataService
    ) {
        return new ngbPatentsSequencesTabService(
            $interval,
            dispatcher,
            ngbTargetPanelService,
            targetDataService,
            projectDataService,
            utilsDataService
        );
    }

    constructor(
        $interval,
        dispatcher,
        ngbTargetPanelService,
        targetDataService,
        projectDataService,
        utilsDataService
    ) {
        Object.assign(this, {
            $interval,
            dispatcher,
            ngbTargetPanelService,
            targetDataService,
            projectDataService,
            utilsDataService
        });
        dispatcher.on('target:identification:sequences:updated', this.setProteins.bind(this));
        dispatcher.on('target:identification:reset', this.resetData.bind(this));
    }

    async getDefaultSettings() {
        const {target_settings: targetSettings} = await this.utilsDataService.getDefaultTrackSettings();
        if (!targetSettings) return;
        const {disable_protein_blast: disableProteinBlast} = targetSettings;
        if (!disableProteinBlast) return;
        this._disableProteinBlast = disableProteinBlast;
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
        this.selectedProtein = this.proteins[0] || {};
        this.loadingProteins = false;
        this.dispatcher.emit('target:identification:patents:proteins:updated');
    }

    async getTableResults() {
        const { searchBy } = this.requestedModel;
        if (searchBy === this.searchByOptions.name) {
            return await this.getPatentsByProteinName();
        }
        if (searchBy === this.searchByOptions.sequence) {
            this._failedResult = false;
            this._errorMessageList = null;
            this._loadingData = false;
            this.setTableResults(this.setTableResultsOnPage());
            return Promise.resolve(true);
        }
    }

    async searchPatentsByProteinName() {
        this.requestedModel = {
            searchBy: this.searchBy,
            proteinId: this.selectedProtein.id,
            proteinName: this.selectedProtein.name
        };
        this.currentPage = 1;
        const success = await this.getPatentsByProteinName();
        if (!success) {
            this.requestedModel = null;
        }
        this.dispatcher.emit('target:identification:patents:protein:pagination:updated');
        return;
    }

    getPatentsByProteinName() {
        const { proteinName } = this.requestedModel;
        const request = {
            name: proteinName,
            page: this.currentPage,
            pageSize: this.pageSize
        };
        if (!proteinName) {
            return new Promise(resolve => {
                this.loadingData = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getPatentsByProteinName(request)
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

    async searchPatentsByProteinSequence() {
        this.currentPage = 1;
        if (!this.searchSequence) {
            this.loadingData = false;
            return;
        }
        this.requestedModel = {
            searchBy: this.searchBy,
            sequence: this.searchSequence,
            originalSequence: this.originalSequence,
        };
        if (this.selectedProtein) {
            this.requestedModel.proteinId = this.selectedProtein.id;
            this.requestedModel.proteinName = this.selectedProtein.name;
        }
        try {
            const {target} = this.ngbTargetPanelService.identificationTarget || {};
            const protein = this.selectedProtein;
            const getBlastTaskBySequence = async () => {
                const task = await this.targetDataService.getBlastTaskBySequence(this.searchSequence);
                if (task && task.id) {
                    this.taskId = task.id;
                    this.updateInterval = this.$interval(this.getBlastSearch.bind(this), this.refreshInterval);
                }
            }
            if (protein.baseString) {
                await getBlastTaskBySequence();
            } else {
                if (target && target.id && protein && protein.id) {
                    const task = await this.targetDataService.getPatentsByProteinId(target.id, protein.id)
                    if (task && task.id) {
                        this.taskId = task.id;
                        const {DONE, FAILURE, CANCELED} = this.blastSearchState;
                        if (task.status === DONE || task.status === FAILURE || task.status === CANCELED) {
                            this.blastStatus = task.status;
                            this.getBlastResultLoad();
                        } else {
                            this.updateInterval = this.$interval(this.getBlastSearch.bind(this), this.refreshInterval);
                        }
                    }
                } else {
                    await getBlastTaskBySequence();
                }
            }
        } catch(err) {
            this._failedResult = true;
            this._errorMessageList = [err.message];
            this._totalPages = 0;
            this._emptyResults = false;
            this._tableResults = null;
            this._loadingData = false;
            this.requestedModel = null;
        }
    }

    async getBlastSearch() {
        if (!this.taskId) return;
        const blast = await this.projectDataService.getBlastSearch(this.taskId);
        const {DONE, FAILURE, CANCELED} = this.blastSearchState;
        if (blast.status === DONE || blast.status === FAILURE || blast.status === CANCELED) {
            this.blastStatus = blast.status;
            this.$interval.cancel(this.updateInterval);
            this.getBlastResultLoad();
        }
    }

    async getBlastResultLoad() {
        const { DONE } = this.blastSearchState;
        if (this.blastStatus === DONE) {
            const result = await this.projectDataService.getBlastResultLoad(this.taskId);
            if (result) {
                this._failedResult = false;
                this._errorMessageList = null;
                this._totalPages = Math.ceil(result.length/this.pageSize);
                this._emptyResults = result.length === 0;
                this._loadingData = false;
                this.setAllResultsBySequence(result);
                this.setTableResults(this.setTableResultsOnPage());
            }
        } else {
            this._failedResult = true;
            this._errorMessageList = ['Error getting results'];
            this._totalPages = 0;
            this._emptyResults = false;
            this._loadingData = false;
            this._tableResults = [];
        }
        this.blastStatus = undefined;
        this.taskId = undefined;
        this.dispatcher.emit('target:identification:patents:protein:search:changed');
        this.dispatcher.emit('target:identification:patents:protein:pagination:updated');
    }

    setAllResultsBySequence(result) {
        const getNcbiUrl = (item) => {
            if (item) {
                const id = item.sequenceAccessionVersion || item.sequenceId;
                return `https://www.ncbi.nlm.nih.gov/protein/${id}`;
            }
            return undefined;
        };
        this._allResults = result.map(i => {
            return {
                'protein': {
                    name: i.sequenceAccessionVersion,
                    url: getNcbiUrl(i)
                },
                'length': i.alignments[0].length,
                'organism': i.organism,
                'name': i.sequenceId,
                'query cover': `${i.queryCoverage}%`,
                'percent identity': `${i.percentIdentity}%`
            };
        });
    }

    setTableResultsOnPage() {
        const allResults = this._allResults;
        if (!allResults || !allResults.length) return [];
        const start = (this.currentPage - 1) * this.pageSize;
        const end = this.currentPage * this.pageSize;
        const results = allResults.slice(start, end);
        return results;
    }

    setTableResults(data) {
        const { searchBy } = this.requestedModel;
        if (searchBy === this.searchByOptions.name) {
            if (data.items) {
                this._tableResults = data.items.map(i => {
                    const protein = { name: i.id };
                    if (i.url) {
                        protein.url = i.url;
                    }
                    return {
                        protein,
                        title: i.title,
                        journal: i.journal,
                        length: i.length,
                        organism: i.organism,
                        name: i.name
                    };
                });
            } else {
                this._tableResults = [];
            }
        }
        if (searchBy === this.searchByOptions.sequence) {
            this._tableResults = data;
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
        if (this.selectedProtein.baseString) {
            this._failedSequence = false;
            this._errorSequence = null;
            this._loadingSequence = false;
            return Promise.resolve(this.selectedProtein.baseString);
        }
        if (this.selectedProtein.id) {
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
        this._failedSequence = false;
        this._errorSequence = null;
        this._loadingSequence = false;
        return Promise.resolve('');
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
        this._allResults = null;
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
