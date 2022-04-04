import ClientPaginationService from '../../../shared/services/clientPaginationService';

const DEFAULT_INTERNAL_PATHWAYS_COLUMNS = [
    'name', 'description', 'source', 'organisms'
];
const DEFAULT_ORDERBY_INTERNAL_PATHWAYS_COLUMNS = {
    'name': 'name',
    'description': 'description',
    'source': 'source',
    'organisms': 'organisms'
};
const INTERNAL_PATHWAYS_COLUMN_TITLES = {
    name: 'Map',
    description: 'Description',
    source: 'Source',
    organisms: 'Species'
};
const DATABASE_SOURCES = {
    CUSTOM: 'Custom',
    BIOCYC: 'BioCyc',
    COLLAGE: 'Collage'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 11;
const blockFilterInternalPathwaysTimeout = 500;

export default class ngbInternalPathwaysTableService extends ClientPaginationService {
    _blockFilterInternalPathways;

    constructor(dispatcher, genomeDataService) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'pathways:internalPathways:page:change');
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.loadSpeciesList();
    }

    _internalPathways;

    get internalPathways() {
        return this._internalPathways;
    }

    _pageError = null;

    get pageError() {
        return this._pageError;
    }

    _isInitialized = false;

    get isInitialized() {
        return this._isInitialized;
    }

    get columnTitleMap() {
        return INTERNAL_PATHWAYS_COLUMN_TITLES;
    }

    get orderByColumns() {
        return DEFAULT_ORDERBY_INTERNAL_PATHWAYS_COLUMNS;
    }

    _internalPathwaysFilter = {};

    get internalPathwaysFilter() {
        return this._internalPathwaysFilter;
    }

    get internalPathwaysColumns() {
        if (!localStorage.getItem('internalPathwaysColumns')) {
            localStorage.setItem('internalPathwaysColumns', JSON.stringify(DEFAULT_INTERNAL_PATHWAYS_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('internalPathwaysColumns'));
    }

    set internalPathwaysColumns(columns) {
        localStorage.setItem('internalPathwaysColumns', JSON.stringify(columns || []));
    }

    _displayInternalPathwaysFilter = true;

    get displayInternalPathwaysFilter() {
        if (this._displayInternalPathwaysFilter !== undefined) {
            return this._displayInternalPathwaysFilter;
        } else {
            this._displayInternalPathwaysFilter = JSON.parse(localStorage.getItem('displayInternalPathwaysFilter')) || false;
            return this._displayInternalPathwaysFilter;
        }
    }

    _speciesList;

    get speciesList() {
        return this._speciesList || [];
    }

    set speciesList(value) {
        this._speciesList = (value || []);
    }

    static instance(dispatcher, genomeDataService) {
        return new ngbInternalPathwaysTableService(dispatcher, genomeDataService);
    }

    async loadSpeciesList() {
        this.speciesList = await this.genomeDataService.getSpeciesList();
        this._isInitialized = true;
        this.dispatcher.emitSimpleEvent('pathways:internalPathways:species:loaded');
    }

    async searchInternalPathways(currentSearch) {
        this._internalPathways = await this.loadInternalPathways(currentSearch);
        this.dispatcher.emitSimpleEvent('internalPathways:result:change');
    }

    async loadInternalPathways(currentSearch) {
        if (currentSearch && currentSearch.rewriteSpecies) {
            this.internalPathwaysFilter.organisms = currentSearch.speciesList;
            currentSearch.speciesList = [];
            currentSearch.rewriteSpecies = false;
        }
        const filter = {
            pagingInfo: {
                pageSize: this.pageSize,
                pageNum: this.currentPage
            },
            sortInfo: this.orderBy ? this.orderBy[0] : null,
            term: currentSearch && currentSearch.search || '',
            taxIds: this.internalPathwaysFilter.organisms || []
        };

        const data = await this.genomeDataService.getInternalPathwaysLoad(filter);
        if (data.error) {
            this.totalPages = 0;
            this.currentPage = FIRST_PAGE;
            this._firstPage = FIRST_PAGE;
            this._pageError = data.message;
            return [];
        } else {
            this._pageError = null;
        }
        this.totalPages = Math.ceil(data.totalCount / this.pageSize);
        if (data && data.items) {
            return data.items.map(this._formatServerToClient);
        } else {
            return [];
        }
    }

    getInternalPathwaysGridColumns() {
        const result = [];
        const columnsList = this.internalPathwaysColumns;
        const headerCells = require('./ngbInternalPathwaysTable_header.tpl.html');
        for (let i = 0; i < columnsList.length; i++) {
            let sortDirection = 0;
            let sortingPriority = 0;
            let columnSettings = null;
            const column = columnsList[i];
            if (this.orderBy) {
                const fieldName = (DEFAULT_ORDERBY_INTERNAL_PATHWAYS_COLUMNS[column] || column);
                const [columnSortingConfiguration] = this.orderBy.filter(o => o.field === fieldName);
                if (columnSortingConfiguration) {
                    sortingPriority = this.orderBy.indexOf(columnSortingConfiguration);
                    sortDirection = columnSortingConfiguration.ascending ? 'asc' : 'desc';
                }
            }
            switch (column) {
                case 'name': {
                    columnSettings = {
                        cellTemplate: `<div class="ui-grid-cell-contents pathways-link"
                                       >{{row.entity.name}}</div>`,
                        enableHiding: false,
                        enableSorting: true,
                        field: 'name',
                        headerCellTemplate: headerCells,
                        name: this.columnTitleMap[column]
                    };
                    break;
                }
                case 'organisms': {
                    columnSettings = {
                        cellTemplate: `<div class="ui-grid-cell-contents"
                                       >{{row.entity.organismsViewValue}}</div>`,
                        enableHiding: false,
                        enableSorting: true,
                        enableFiltering: true,
                        field: 'organisms',
                        headerCellTemplate: headerCells,
                        name: this.columnTitleMap[column],
                        filterApplied: () => this.internalPathwaysFieldIsFiltered(column),
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.clearInternalPathwaysFieldFilter(column),
                                shown: () => this.internalPathwaysFieldIsFiltered(column)
                            }
                        ],
                    };
                    break;
                }
                default: {
                    columnSettings = {
                        enableHiding: false,
                        field: column,
                        minWidth: 40,
                        headerCellTemplate: headerCells,
                        name: this.columnTitleMap[column],
                        width: '*'
                    };
                    break;
                }
            }
            if (columnSettings) {
                if (sortDirection) {
                    columnSettings.sort = {
                        direction: sortDirection,
                        priority: sortingPriority
                    };
                }
                result.push(columnSettings);
            }
        }
        return result;
    }

    clearInternalPathwaysFilter() {
        if (this._blockFilterInternalPathways) {
            clearTimeout(this._blockFilterInternalPathways);
            this._blockFilterInternalPathways = null;
        }
        this._internalPathwaysFilter = {};
        this.dispatcher.emit('pathways:internalPathways:refresh');
        this._blockFilterInternalPathways = setTimeout(() => {
            this._blockFilterInternalPathways = null;
        }, blockFilterInternalPathwaysTimeout);
    }

    internalPathwaysFieldIsFiltered(fieldName) {
        return this.internalPathwaysFilter[fieldName] !== undefined;
    }

    clearInternalPathwaysFieldFilter(fieldName) {
        this.internalPathwaysFilter[fieldName] = undefined;
        this.dispatcher.emit('pathways:internalPathways:refresh');
    }

    canScheduleFilterInternalPathways() {
        return !this._blockFilterInternalPathways;
    }

    scheduleFilterInternalPathways() {
        if (this._blockFilterInternalPathways) {
            return;
        }
        this.dispatcher.emit('pathways:internalPathways:refresh');
    }

    _formatServerToClient(internalPathways) {
        const result = {
            id: internalPathways.pathwayId,
            name: internalPathways.prettyName || internalPathways.name,
            description: internalPathways.pathwayDesc,
            databaseSource: internalPathways.databaseSource,
            source: DATABASE_SOURCES[internalPathways.databaseSource] || internalPathways.databaseSource,
            organisms: internalPathways.organisms
        };
        if (internalPathways.organisms) {
            result.organismsViewValue = internalPathways.organisms
                .map(organism => organism.speciesName || organism.taxId)
                .join(', ');
        }
        return result;
    }
}
