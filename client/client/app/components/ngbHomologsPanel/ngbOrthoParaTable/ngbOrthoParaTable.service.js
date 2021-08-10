const DEFAULT_ORTHO_PARA_COLUMNS = [
    'gene', 'protein', 'info'
];
const DEFAULT_ORDERBY_ORTHO_PARA_COLUMNS = {
    'gene': 'gene',
    'protein': 'protein',
    'info': 'info'
};
const ORTHO_PARA_COLUMN_TITLES = {
    gene: 'Gene Name',
    protein: 'Protein Name',
    info: 'Info'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 15;

export default class ngbOrthoParaTableService {

    constructor(dispatcher, genomeDataService) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    _orthoPara;

    get orthoPara() {
        return this._orthoPara;
    }

    _firstPage = FIRST_PAGE;

    get firstPage() {
        return this._firstPage;
    }

    set firstPage(value) {
        this._firstPage = value;
    }

    _totalPages = FIRST_PAGE;

    get totalPages() {
        return this._totalPages;
    }

    _currentPage = FIRST_PAGE;

    get currentPage() {
        return this._currentPage;
    }

    set currentPage(value) {
        this._currentPage = value;
    }

    _pageSize = PAGE_SIZE;

    get pageSize() {
        return this._pageSize;
    }

    set pageSize(value) {
        this._pageSize = value;
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
        return ORTHO_PARA_COLUMN_TITLES;
    }

    get orderByColumns() {
        return DEFAULT_ORDERBY_ORTHO_PARA_COLUMNS;
    }

    get orthoParaColumns() {
        if (!localStorage.getItem('orthoParaColumns')) {
            localStorage.setItem('orthoParaColumns', JSON.stringify(DEFAULT_ORTHO_PARA_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('orthoParaColumns'));
    }

    set orthoParaColumns(columns) {
        localStorage.setItem('orthoParaColumns', JSON.stringify(columns || []));
    }

    static instance(dispatcher, genomeDataService) {
        return new ngbOrthoParaTableService(dispatcher, genomeDataService);
    }

    async updateOrthoPara() {
        this._orthoPara = await this.loadOrthoPara(this.currentPage);
    }

    changePage(page) {
        this.currentPage = page;
        this.dispatcher.emit('homologs:orthoPara:page:change', page);
    }

    async loadOrthoPara(page) {
        const filter = {
            pagingInfo: {
                pageNum: page,
                pageSize: this.pageSize
            },
            sortInfos: this.orderBy
        };
        const data = await this.genomeDataService.getOrthoParaLoad(filter);
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

    getOrthoParaGridColumns() {
        const result = [];
        const columnsList = this.orthoParaColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let sortDirection = 0;
            let sortingPriority = 0;
            let columnSettings = null;
            const column = columnsList[i];
            if (this.orderBy) {
                const fieldName = (DEFAULT_ORDERBY_ORTHO_PARA_COLUMNS[column] || column);
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

    _formatServerToClient(orthoPara) {
        return {
            gene: orthoPara.gene,
            protein: orthoPara.protein,
            info: orthoPara.info
        };
    }
}
