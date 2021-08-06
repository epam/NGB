import ClientPaginationService from '../../../shared/services/clientPaginationService';

const DEFAULT_HOMOLOGENE_COLUMNS = [
    'gene', 'protein', 'info'
];
const DEFAULT_ORDERBY_HOMOLOGENE_COLUMNS = {
    'gene': 'gene',
    'protein': 'protein',
    'info': 'info'
};
const HOMOLOGENE_COLUMN_TITLES = {
    gene: 'Gene Name',
    protein: 'Protein Name',
    info: 'Info'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 15;

export default class ngbHomologeneTableService extends ClientPaginationService {

    constructor(dispatcher, genomeDataService) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'homologs:homologene:page:change');
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    _homologene;

    get homologene() {
        return this._homologene;
    }

    _pageError = null;

    get pageError() {
        return this._pageError;
    }

    _orderBy = null;

    get orderBy() {
        return this._orderBy;
    }

    set orderBy(orderBy) {
        this._orderBy = orderBy;
    }

    get columnTitleMap() {
        return HOMOLOGENE_COLUMN_TITLES;
    }

    get orderByColumns() {
        return DEFAULT_ORDERBY_HOMOLOGENE_COLUMNS;
    }

    get homologeneColumns() {
        if (!localStorage.getItem('homologeneColumns')) {
            localStorage.setItem('homologeneColumns', JSON.stringify(DEFAULT_HOMOLOGENE_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('homologeneColumns'));
    }

    set homologeneColumns(columns) {
        localStorage.setItem('homologeneColumns', JSON.stringify(columns || []));
    }

    static instance(dispatcher, genomeDataService) {
        return new ngbHomologeneTableService(dispatcher, genomeDataService);
    }

    async updateHomologene() {
        this._homologene = await this.loadHomologene(this.currentPage);
    }

    async loadHomologene(page) {
        const filter = {
            pagingInfo: {
                pageNum: page,
                pageSize: this.pageSize
            },
            sortInfos: this.orderBy
        };
        const data = await this.genomeDataService.getHomologeneLoad(filter);
        if (data.error) {
            this._totalPages = 0;
            this.currentPage = FIRST_PAGE;
            this._firstPage = FIRST_PAGE;
            this._pageError = data.message;
            return [];
        } else {
            this._pageError = null;
        }
        this._totalPages = Math.ceil(data.length / this.pageSize);
        let filteredData = [];
        if (data) {
            filteredData = data;
            filteredData.forEach((value, key) => filteredData[key] = this._formatServerToClient(value));
        }
        return filteredData;
    }

    getHomologeneGridColumns() {
        const result = [];
        const columnsList = this.homologeneColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let sortDirection = 0;
            let sortingPriority = 0;
            let columnSettings = null;
            const column = columnsList[i];
            if (this.orderBy) {
                const fieldName = (DEFAULT_ORDERBY_HOMOLOGENE_COLUMNS[column] || column);
                const [columnSortingConfiguration] = this.orderBy.filter(o => o.field === fieldName);
                if (columnSortingConfiguration) {
                    sortingPriority = this.orderBy.indexOf(columnSortingConfiguration);
                    sortDirection = columnSortingConfiguration.ascending ? 'asc' : 'desc';
                }
            }
            switch (column) {
                case 'gene': {
                    columnSettings = {
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link"
                                       >{{row.entity.gene}}</div>`,
                        enableHiding: false,
                        field: 'gene',
                        name: this.columnTitleMap[column]
                    };
                    break;
                }
                default: {
                    columnSettings = {
                        enableHiding: false,
                        field: column,
                        minWidth: 40,
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

    _formatServerToClient(homologene) {
        return {
            gene: homologene.gene,
            protein: homologene.protein,
            info: homologene.info
        };
    }
}
