import ClientPaginationService from '../../../shared/services/clientPaginationService';

const DEFAULT_INTERNAL_PATHWAYS_COLUMNS = [
    'name', 'description', 'source'
];
const DEFAULT_ORDERBY_INTERNAL_PATHWAYS_COLUMNS = {
    'name': 'name',
    'description': 'description',
    'source': 'source'
};
const INTERNAL_PATHWAYS_COLUMN_TITLES = {
    name: 'Map',
    description: 'Description',
    source: 'Source'
};
const DATABASE_SOURCES = {
    CUSTOM: 'Custom',
    BIOCYC: 'BioCyc',
    COLLAGE: 'Collage'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 11;

export default class ngbInternalPathwaysTableService extends ClientPaginationService {
    constructor(dispatcher, genomeDataService) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'pathways:internalPathways:page:change');
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    _internalPathways;

    get internalPathways() {
        return this._internalPathways;
    }

    _pageError = null;

    get pageError() {
        return this._pageError;
    }

    get columnTitleMap() {
        return INTERNAL_PATHWAYS_COLUMN_TITLES;
    }

    get orderByColumns() {
        return DEFAULT_ORDERBY_INTERNAL_PATHWAYS_COLUMNS;
    }

    get internalPathwaysColumns() {
        if (!localStorage.getItem('internalPathwaysColumnNames')) {
            localStorage.setItem('internalPathwaysColumnNames', JSON.stringify(DEFAULT_INTERNAL_PATHWAYS_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('internalPathwaysColumnNames'));
    }

    set internalPathwaysColumns(columns) {
        localStorage.setItem('internalPathwaysColumnNames', JSON.stringify(columns || []));
    }

    static instance(dispatcher, genomeDataService) {
        return new ngbInternalPathwaysTableService(dispatcher, genomeDataService);
    }

    async searchInternalPathways(currentSearch) {
        this._internalPathways = await this.loadInternalPathways(currentSearch);
        this.dispatcher.emitSimpleEvent('internalPathways:result:change');
    }

    async loadInternalPathways(currentSearch) {
        const filter = {
            pagingInfo: {
                pageSize: this.pageSize,
                pageNum: this.currentPage
            },
            sortInfo: this.orderBy ? this.orderBy[0] : null,
            term: currentSearch
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

    _formatServerToClient(internalPathways) {
        return {
            id: internalPathways.pathwayId,
            name: internalPathways.prettyName || internalPathways.name,
            description: internalPathways.pathwayDesc,
            databaseSource: internalPathways.databaseSource,
            source: DATABASE_SOURCES[internalPathways.databaseSource] || internalPathways.databaseSource,
            organisms: internalPathways.organisms
        };
    }
}
