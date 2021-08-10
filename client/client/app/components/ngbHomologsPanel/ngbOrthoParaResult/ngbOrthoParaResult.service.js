const DEFAULT_COLUMNS = [
    'name', 'species', 'aa', 'domains', 'type'
];

const ORTHO_PARA_COLUMN_TITLES = {
};

const FIRST_PAGE = 1;
const PAGE_SIZE = 15;

export default class ngbOrthoParaResultService {
    _searchResultTableLoading = true;

    constructor(dispatcher, genomeDataService) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    _searchResultTableError = null;

    get searchResultTableError() {
        return this._searchResultTableError;
    }

    _orthoParaResult = {};

    get orthoParaResult() {
        return this._orthoParaResult;
    }

    get orthoParaResultColumns() {
        if (localStorage.getItem('orthoParaResultColumns') === null || localStorage.getItem('orthoParaResultColumns') === undefined) {
            localStorage.setItem('orthoParaResultColumns', JSON.stringify(DEFAULT_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('orthoParaResultColumns'));
    }

    set orthoParaResultColumns(columns) {
        localStorage.setItem('orthoParaResultColumns', JSON.stringify(columns || []));
    }

    get columnTitleMap() {
        return ORTHO_PARA_COLUMN_TITLES;
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

    static instance(dispatcher, genomeDataService) {
        return new ngbOrthoParaResultService(dispatcher, genomeDataService);
    }

    changePage(page) {
        this.currentPage = page;
        this.dispatcher.emit('homologs:orthoPara:result:page:change', page);
    }

    getOrthoParaResultGridColumns() {
        const headerCells = require('./ngbOrthoParaResultTable/ngbOrthoParaResultTable_header.tpl.html');

        const result = [];
        const columnsList = this.orthoParaResultColumns;
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column) {
                case 'domains': {
                    result.push({
                        cellTemplate: '<ngb-homologs-domains domains="row.entity.domainsObj"></ngb-homologs-domains>',
                        enableHiding: false,
                        field: 'domainsObj',
                        headerCellTemplate: headerCells,
                        minWidth: 300,
                        name: column
                    });
                    break;
                }
                case 'name': {
                    result.push({
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link"
                                       >{{row.entity.name}}</div>`,
                        enableHiding: false,
                        field: 'name',
                        headerCellTemplate: headerCells,
                        name: column
                    });
                    break;
                }
                default: {
                    result.push({
                        enableHiding: false,
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.columnTitleMap[column] || column,
                        width: '*'
                    });
                    break;
                }
            }
        }
        return result;
    }

    async updateSearchResult(searchId) {
        this._orthoParaResult = await this.loadOrthoParaResult(searchId);
        this.dispatcher.emitSimpleEvent('orthoPara:result:change');
    }

    async loadOrthoParaResult(searchId) {
        const data = await this.genomeDataService.getOrthoParaResultLoad(searchId);
        let maxHomologLength = 0;
        const colors = {
            'A': 'red',
            'B': 'blue',
            'C': 'green',
            'D': 'yellow',
        };
        if (data.error) {
            this._totalPages = 0;
            this.currentPage = FIRST_PAGE;
            this._firstPage = FIRST_PAGE;
            this._searchResultTableLoading = false;
            this._searchResultTableError = data.message;
            return [];
        } else {
            this._searchResultTableError = null;
        }
        this._totalPages = Math.ceil(data.length / this.pageSize);
        if (data) {
            data.forEach((value, key) => {
                data[key] = this._formatServerToClient(value);
                if (maxHomologLength < data[key].aa) {
                    maxHomologLength = data[key].aa;
                }
            });
            data.forEach((value, key) => {
                data[key].domainsObj = {
                    domains: data[key].domains.map(d => ({...d, color: colors[d.name]})),
                    homologLength: data[key].aa,
                    maxHomologLength: maxHomologLength
                };
                // delete data[key].domains;
            });
        }
        return data || [];
    }

    _formatServerToClient(result) {
        return {
            name: result.name,
            species: result.species,
            aa: result.aa,
            domains: result.domains,
            type: result.type
        };
    }
}
