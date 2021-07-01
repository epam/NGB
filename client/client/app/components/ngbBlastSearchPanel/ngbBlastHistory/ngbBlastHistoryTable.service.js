import utcDateFactory from '../ngbBlastSearch.date.filter';

const utcDateFormatter = utcDateFactory();

const DEFAULT_BLAST_HISTORY_COLUMNS = [
    'id', 'title', 'currentState', 'submitted', 'duration', 'actions'
];
const DEFAULT_ORDERBY_HISTORY_COLUMNS = {
    'submitted': 'created_date',
    'id': 'task_id',
    'title': 'title',
    'currentState': 'status'
};
const HISTORY_COLUMN_TITLES = {
    id: 'Task Id',
    title: 'Task Title',
    currentState: 'Current State',
    submitted: 'Submitted At',
    duration: 'Duration',
    actions: ''
};
const blastSearchState = {
    DONE: 'DONE',
    FAILURE: 'FAILURE',
    SEARCHING: 'SEARCHING',
    CANCELED: 'CANCELED'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 15;
const REFRESH_INTERVAL_SEC = 5;

export default class ngbBlastHistoryTableService {

    static instance(dispatcher, projectDataService) {
        return new ngbBlastHistoryTableService(dispatcher, projectDataService);
    }

    _blastHistory;
    _firstPageHistory = FIRST_PAGE;
    _totalPages = FIRST_PAGE;
    _currentPageHistory = FIRST_PAGE;
    _historyPageSize = PAGE_SIZE;
    _historyPageError = null;
    _orderByHistory = null;

    constructor(dispatcher, projectDataService) {
        this.dispatcher = dispatcher;
        this.projectDataService = projectDataService;
    }

    get firstPageHistory() {
        return this._firstPageHistory;
    }

    set firstPageHistory(value) {
        this._firstPageHistory = value;
    }

    get totalPages() {
        return this._totalPages;
    }

    get blastSearchState() {
        return blastSearchState;
    }

    get historyColumnTitleMap() {
        return HISTORY_COLUMN_TITLES;
    }

    get currentPageHistory() {
        return this._currentPageHistory;
    }

    set currentPageHistory(value) {
        this._currentPageHistory = value;
    }

    get historyPageSize() {
        return this._historyPageSize;
    }

    set historyPageSize(value) {
        this._historyPageSize = value;
    }

    get refreshInterval() {
        return REFRESH_INTERVAL_SEC * 1000;
    }

    get historyPageError() {
        return this._historyPageError;
    }

    get orderByHistory() {
        return this._orderByHistory;
    }

    set orderByHistory(orderByHistory) {
        this._orderByHistory = orderByHistory;
    }

    get orderByColumnsHistory() {
        return DEFAULT_ORDERBY_HISTORY_COLUMNS;
    }

    get blastHistory() {
        return this._blastHistory;
    }

    async updateSearchHistory() {
        this._blastHistory = await this.loadBlastHistory(this.currentPageHistory);
    }

    async deleteBlastSearch(id) {
        this.projectDataService.deleteBlastSearch(id).then(async () => {
            this._blastHistory = await this.loadBlastHistory(this.currentPageHistory);
        });
    }

    async cancelBlastSearch(id) {
        this.projectDataService.cancelBlastSearch(id).then(async () => {
            this._blastHistory = await this.loadBlastHistory(this.currentPageHistory);
        });
    }

    clearSearchHistory() {
        this.projectDataService.deleteBlastSearchHistory().then(async () => {
            this._blastHistory = await this.loadBlastHistory();
        });
    }

    downloadResults(id) {
        return this.projectDataService.downloadBlastResults(id);
    }

    changePage(page) {
        this.currentPageHistory = page;
        this.dispatcher.emit('blast:history:page:change', page);
    }

    async loadBlastHistory(page) {
        const filter = {
            pagingInfo: {
                pageNum: page,
                pageSize: this.historyPageSize
            },
            sortInfos: this.orderByHistory
        };
        const data = await this.projectDataService.getBlastHistoryLoad(filter);
        if (data.error) {
            this._totalPages = 0;
            this.currentPageHistory = FIRST_PAGE;
            this._firstPageHistory = FIRST_PAGE;
            this._historyPageError = data.message;
            return [];
        } else {
            this._historyPageError = null;
        }
        this._totalPages = Math.ceil(data.totalCount / this.historyPageSize);
        let filteredData = [];
        if (data.blastTasks) {
            filteredData = data.blastTasks;
            filteredData.forEach((value, key) => filteredData[key] = this._formatServerToClient(value));
        }
        return filteredData;
    }

    getBlastHistoryGridColumns() {
        const actionsCell = require('./ngbBlastHistoryTable_actions.tpl.html');
        const headerCells = require('./ngbBlastHistoryTable_header.tpl.html');

        const result = [];
        const columnsList = this.blastHistoryColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let sortDirection = 0;
            let sortingPriority = 0;
            let columnSettings = null;
            const column = columnsList[i];
            if (this.orderByHistory) {
                const fieldName = (DEFAULT_ORDERBY_HISTORY_COLUMNS[column] || column);
                const [columnSortingConfiguration] = this.orderByHistory.filter(o => o.field === fieldName);
                if (columnSortingConfiguration) {
                    sortingPriority = this.orderByHistory.indexOf(columnSortingConfiguration);
                    sortDirection = columnSortingConfiguration.ascending ? 'asc' : 'desc';
                }
            }
            switch (column) {
                case 'id': {
                    columnSettings = {
                        cellTemplate: `<div class="ui-grid-cell-contents"
                                        ng-class="row.entity.isResult
                                        ? 'search-result-link'
                                        : 'search-result-not-link'"
                                       >{{row.entity.id}}</div>`,
                        enableHiding: false,
                        field: 'id',
                        headerCellTemplate: headerCells,
                        minWidth: 60,
                        maxWidth: 80,
                        name: this.historyColumnTitleMap[column]
                    };
                    break;
                }
                case 'currentState': {
                    columnSettings = {
                        cellTemplate: `<div class="ui-grid-cell-contents">
                                        {{grid.appScope.$ctrl.statusViews[row.entity.currentState]}}
                                       </div>`,
                        enableHiding: false,
                        field: 'currentState',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.historyColumnTitleMap[column]
                    };
                    break;
                }
                case 'submitted': {
                    columnSettings = {
                        cellFilter: 'date:"short"',
                        enableHiding: false,
                        field: 'submitted',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.historyColumnTitleMap[column]
                    };
                    break;
                }
                case 'duration': {
                    columnSettings = {
                        cellFilter: 'duration:this',
                        enableHiding: false,
                        enableSorting: false,
                        enableColumnMenu: false,
                        field: 'duration',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.historyColumnTitleMap[column]
                    };
                    break;
                }
                case 'actions': {
                    columnSettings = {
                        cellTemplate: actionsCell,
                        enableSorting: false,
                        field: 'id',
                        headerCellTemplate: '<span></span>',
                        maxWidth: 70,
                        minWidth: 60,
                        name: this.historyColumnTitleMap[column]
                    };
                    break;
                }
                default: {
                    columnSettings = {
                        enableHiding: false,
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.historyColumnTitleMap[column],
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

    get blastHistoryColumns() {
        if (!localStorage.getItem('blastHistoryColumns')) {
            localStorage.setItem('blastHistoryColumns', JSON.stringify(DEFAULT_BLAST_HISTORY_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('blastHistoryColumns'));
    }

    set blastHistoryColumns(columns) {
        localStorage.setItem('blastHistoryColumns', JSON.stringify(columns || []));
    }

    _formatServerToClient(search) {
        let duration, state;
        switch (search.status) {
            case 'CREATED':
            case 'SUBMITTED':
            case 'RUNNING': {
                state = blastSearchState.SEARCHING;
                break;
            }
            case 'FAILED': {
                state = blastSearchState.FAILURE;
                break;
            }
            case 'DONE': {
                state = blastSearchState.DONE;
                break;
            }
            case 'CANCELED': {
                state = blastSearchState.CANCELED;
                break;
            }
        }
        if (state === blastSearchState.SEARCHING) {
            duration = Date.now() - +(utcDateFormatter(search.createdDate));
        } else {
            duration = +new Date(search.endDate) - +new Date(search.createdDate);
        }
        return {
            id: search.id,
            title: search.title,
            currentState: state,
            submitted: utcDateFormatter(search.createdDate),
            duration: Math.ceil(duration / 1000),
            isResult: state === blastSearchState.DONE || state === blastSearchState.FAILURE,
            isInProgress: state === blastSearchState.SEARCHING
        };
    }
}
