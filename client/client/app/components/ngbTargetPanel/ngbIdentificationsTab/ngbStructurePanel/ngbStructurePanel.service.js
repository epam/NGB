import ngbConstants from '../../../../../constants';

const SOURCE_OPTIONS = {
    PROTEIN_DATA_BANK: 'Protein Data Bank',
    LOCAL_FILES: 'Local Files'
};

const EXPORT_SOURCE = 'STRUCTURES';

const PAGE_SIZE = 10;

const PROTEIN_DATA_BANK_COLUMNS = ['id', 'name', 'method', 'source', 'resolution', 'chains'];
const LOCAL_FILES_COLUMNS = ['id', 'name', 'owner'];

const FIELDS = {
    id: 'ENTRY_ID',
    resolution: 'RESOLUTION',
    name: 'NAME'
};

const LOCAL_FILES_FILTER_FIELDS = {
    name: 'prettyName',
    id: 'name',
    owner: 'owner'
};

const LOCAL_FILES_SORT_FIELDS = {
    name: 'pretty_name',
    id: 'name',
    owner: 'owner'
};

const LOCAL_FILES_DEFAULT_SORT = [{
    field: 'name',
    ascending: true
}];

const serviceA = document.createElement('a');

function getDataServiceEndpointAbsoluteURL(relativeURL = '') {
    try {
        let base = ngbConstants.urlPrefix || '';
        if (base && base.length && !base.endsWith('/')) {
            base = base.concat('/');
        }
        let restapi = 'restapi';
        if (!relativeURL.startsWith('/')) {
            restapi = restapi.concat('/');
        }
        serviceA.href = `${base}${restapi}${relativeURL}`;
        return serviceA.href;
    } catch (_) {
        // empty
    }
    return undefined;
}

export default class ngbStructurePanelService {

    get sourceOptions() {
        return SOURCE_OPTIONS;
    }
    get exportSource() {
        return EXPORT_SOURCE;
    }
    get pageSize() {
        return PAGE_SIZE;
    }
    get proteinDataBankColumns() {
        return PROTEIN_DATA_BANK_COLUMNS;
    }
    get localFilesColumns() {
        return LOCAL_FILES_COLUMNS;
    }
    get columnsList() {
        if (this._sourceModel === this.sourceOptions.LOCAL_FILES) {
            return this.localFilesColumns;
        }
        if (this._sourceModel === this.sourceOptions.PROTEIN_DATA_BANK) {
            return this.proteinDataBankColumns;
        }
        return [];
    }

    get fields() {
        return FIELDS;
    }

    get localFilesFilterFields() {
        return LOCAL_FILES_FILTER_FIELDS;
    }
    get localFilesSortFields() {
        return LOCAL_FILES_SORT_FIELDS;
    }

    get localFilesDefaultSort() {
        return LOCAL_FILES_DEFAULT_SORT;
    }

    _totalPages = 0;
    _currentPage = 1;
    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _emptyResults = false;
    _structureResults = null;
    _filterInfo = null;
    _sortInfo = null;
    _selectedPdbId = null;
    _selectedPdbReference = null;// identifier or URL
    _pdbDescriptions = null;
    _pdbDescriptionLoading = false;
    _pdbDescriptionFailed = false;
    _pdbDescriptionErrorMessageList = null;
    _descriptionDone = false;

    get totalPages() {
        return this._totalPages;
    }
    get currentPage() {
        return this._currentPage;
    }
    set currentPage(value) {
        this._currentPage = value;
    }

    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
    }
    get failedResult() {
        return this._failedResult;
    }
    get errorMessageList() {
        return this._errorMessageList;
    }
    get emptyResults() {
        return this._emptyResults;
    }
    get structureResults() {
        return this._structureResults;
    }

    get sourceModel() {
        return this._sourceModel;
    }
    set sourceModel(value) {
        this._sourceModel = value;
    }

    get filterInfo() {
        return this._filterInfo;
    }
    set filterInfo(value) {
        this._filterInfo = value;
    }

    get sortInfo() {
        return this._sortInfo;
    }
    set sortInfo(value) {
        this._sortInfo = value;
    }

    get selectedPdbId() {
        return this._selectedPdbId;
    }
    set selectedPdbId(value) {
        this._selectedPdbId = value;
    }
    get selectedPdbReference() {
        return this._selectedPdbReference;
    }
    set selectedPdbReference(reference) {
        this._selectedPdbReference = reference;
    }
    get pdbDescriptions() {
        return this._pdbDescriptions;
    }
    get pdbDescriptionLoading() {
        return this._pdbDescriptionLoading;
    }
    get pdbDescriptionFailed() {
        return this._pdbDescriptionFailed;
    }
    get pdbDescriptionErrorMessageList() {
        return this._pdbDescriptionErrorMessageList;
    }
    get descriptionDone() {
        return this._descriptionDone;
    }
    set descriptionDone(value) {
        this._descriptionDone = value;
    }
    get geneIdsOfInterest() {
        return this.ngbTargetPanelService.geneIdsOfInterest;
    }
    get translationalGeneIds() {
        return this.ngbTargetPanelService.translationalGeneIds;
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbStructurePanelService(dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {ngbTargetPanelService, targetDataService});
        this._sourceModel = this.sourceOptions.PROTEIN_DATA_BANK;
        dispatcher.on('target:identification:changed', this.targetChanged.bind(this));
    }

    get geneIds() {
        return this.ngbTargetPanelService.genesIds;
    }

    async targetChanged() {
        this.resetStructureData();
    }

    setStructureResults(data) {
        if (this._sourceModel === this.sourceOptions.LOCAL_FILES) {
            this._structureResults = data.map(item => ({
                id: {
                    name: item.name,
                    url: item.pdbFileId
                        ? getDataServiceEndpointAbsoluteURL(`pdb/content/${item.pdbFileId}`)
                        : undefined
                },
                local: true,
                name: item.prettyName,
                owner: item.owner,
            }));
        }
        if (this._sourceModel === this.sourceOptions.PROTEIN_DATA_BANK) {
            this._structureResults = data.map(item => ({
                id: {
                    name: item.id,
                    url: item.url
                },
                name: item.name,
                method: item.method,
                source: item.source,
                resolution: item.resolution,
                chains: (item.proteinChains || []).join('/')
            }));
        }
    }

    getStructureRequest () {
        if (this._sourceModel === this.sourceOptions.LOCAL_FILES) {
            const request = {
                pagingInfo: {
                    pageSize: this.pageSize,
                    pageNum: this.currentPage
                },
                geneIds: [this.geneIds[0]]
            };
            if (this.sortInfo && this.sortInfo.length) {
                request.sortInfos = this.sortInfo.map(i => ({
                    field: this.localFilesSortFields[i.field],
                    ascending: i.ascending
                }))
            } else {
                request.sortInfos = this.localFilesDefaultSort;
            }
            if (this._filterInfo) {
                if (this._filterInfo.id) {
                    request[this.localFilesFilterFields.id] = this._filterInfo.id;
                }
                if (this._filterInfo.name) {
                    request[this.localFilesFilterFields.name] = this._filterInfo.name;
                }
                if (this._filterInfo.owner) {
                    request[this.localFilesFilterFields.owner] = this._filterInfo.owner;
                }
            }
            return request;
        }
        if (this._sourceModel === this.sourceOptions.PROTEIN_DATA_BANK) {
            const request = {
                geneIds: this.geneIds,
                page: this.currentPage,
                pageSize: this.pageSize,
                orderBy: this.fields.id,
                reverse: false
            };
            if (this._filterInfo) {
                if (this._filterInfo.id) {
                    request.entryIds = [this._filterInfo.id];
                }
                if (this._filterInfo.name) {
                    request.name = this._filterInfo.name;
                }
            }
            return request;
        }
    }

    getStructureResults() {
        const request = this.getStructureRequest();
        if (!request) {
            return new Promise(resolve => {
                this._loadingData = false;
                resolve(true);
            });
        }
        const source = this.sourceModel;
        return new Promise(resolve => {
            this.targetDataService.getStructureResults(request, source)
                .then(([data, totalCount]) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyResults = totalCount === 0;
                    this.setStructureResults(data);
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._totalPages = 0;
                    this._emptyResults = false;
                    this._loadingData = false;
                    resolve(false);
                });
        });
    }

    async getPdbDescription(entity) {
        if (!entity) {
            return;
        }
        const {
            id = {},
            local = false
        } = entity;
        const {
            name: pdbId,
            url
        } = id;
        this._pdbDescriptionLoading = true;
        return new Promise(resolve => {
            this.targetDataService.getPdbDescription(pdbId)
                .then(data => {
                    this._pdbDescriptionFailed = false;
                    this._pdbDescriptionErrorMessageList = null;
                    this._selectedPdbId = pdbId;
                    this._selectedPdbReference = local && url ? url : pdbId;
                    this._pdbDescriptions = data;
                    this._pdbDescriptionLoading = false;
                    resolve(true);
                })
                .catch(err => {
                    this._selectedPdbId = null;
                    this._selectedPdbReference = null;
                    this._pdbDescriptions = null;
                    this._pdbDescriptionFailed = true;
                    this._pdbDescriptionErrorMessageList = [err.message];
                    this._pdbDescriptionLoading = false;
                    resolve(false);
                });
        });
    }

    setFilter(field, value) {
        const filter = {...(this._filterInfo || {})};
        if (value) {
            filter[field] = value;
        } else {
            delete filter[field];
        }
        this._filterInfo = filter;
    }

    resetStructureData() {
        this._totalPages = 0;
        this._currentPage = 1;
        this._sortInfo = null;
        this._filterInfo = null;
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
        this._structureResults = null;
        this._descriptionDone = false;
    }

    exportResults() {
        const source = this.exportSource;
        if (!this.geneIdsOfInterest || !this.translationalGeneIds) {
            return new Promise(resolve => {
                resolve(true);
            });
        }
        return this.targetDataService.getTargetExport(this.geneIdsOfInterest, this.translationalGeneIds, source);
    }
}
