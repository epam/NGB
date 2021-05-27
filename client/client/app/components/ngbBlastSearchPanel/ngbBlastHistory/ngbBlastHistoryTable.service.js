import {camelPad} from '../../../shared/utils/String';

const DEFAULT_BLAST_HISTORY_COLUMNS = [
    'id', 'title', 'currentState', 'submitted', 'duration', 'actions'
];
const DEFAULT_ORDERBY_HISTORY_COLUMNS = {
    'submitted': 'CREATED_DATE',
    'id': 'task_id',
    'title': 'TITLE'
};
const blastSearchState = {
    DONE: 'DONE',
    FAILURE: 'FAILURE',
    SEARCHING: 'SEARCHING'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 5;
const REFRESH_INTERVAL_SEC = 10;

export default class ngbBlastHistoryTableService {

    static instance(projectDataService) {
        return new ngbBlastHistoryTableService(projectDataService);
    }

    _blastHistory;
    _firstPageHistory = FIRST_PAGE;
    _totalPages = FIRST_PAGE;
    _currentPageHistory = FIRST_PAGE;
    _historyPageError = null;
    _orderByHistory = null;

    constructor(projectDataService) {
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

    get currentPageHistory() {
        return this._currentPageHistory;
    }

    set currentPageHistory(value) {
        this._currentPageHistory = value;
    }

    get historyPageSize() {
        return PAGE_SIZE;
    }

    get refreshInterval() {
        return REFRESH_INTERVAL_SEC*1000;
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


    set blastHistoryColumns(columns) {
        localStorage.setItem('blastHistoryColumns', JSON.stringify(columns || []));
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

    async clearSearchHistory() {
        this._blastHistory = await this.loadBlastHistory();
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
            this._hasMoreHistory = true;
            this._historyPageError = data.message;
            // this.dispatcher.emit('blast:history:page:loading:finished');
            return [];
        } else {
            this._historyPageError = null;
        }
        this._totalPages = Math.ceil(data.totalCount/this.historyPageSize);
        const filteredData = data.blastTasks.filter(blastSearch => blastSearch.status !== 'CANCELLED');
        filteredData.forEach((value, key) => filteredData[key] = this._formatServerToClient(value));
        return filteredData;
    }

    getBlastHistoryGridColumns() {
        const actionsCell = require('./ngbBlastHistoryTable_actions.tpl.html');
        const headerCells = require('./ngbBlastHistoryTable_header.tpl.html');

        const result = [];
        const columnsList = this.blastHistoryColumns;
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column) {
                case 'id': {
                    result.push({
                        cellTemplate: `<div class="ui-grid-cell-contents"
                                        ng-class="row.entity.isInProcess 
                                        ? 'search-result-in-progress' 
                                        : 'search-result-link'"
                                       >{{row.entity.id}}</div>`,
                        enableHiding: false,
                        field: 'id',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: 'id'
                    });
                    break;
                }
                case 'currentState': {
                    result.push({
                        cellTemplate: `<div class="ui-grid-cell-contents">
                                        {{grid.appScope.$ctrl.statusViews[row.entity.currentState]}}
                                       </div>`,
                        enableHiding: false,
                        field: 'currentState',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: 'currentState'
                    });
                    break;
                }
                case 'submitted': {
                    result.push({
                        cellFilter: 'date:"short"',
                        enableHiding: false,
                        field: 'submitted',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: 'submitted'
                    });
                    break;
                }
                case 'duration': {
                    result.push({
                        cellFilter: 'duration:this',
                        enableHiding: false,
                        enableSorting: false,
                        field: 'duration',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: 'duration'
                    });
                    break;
                }
                case 'actions': {
                    result.push({
                        cellTemplate: actionsCell,
                        enableSorting: false,
                        field: 'id',
                        headerCellTemplate: '<span></span>',
                        maxWidth: 70,
                        minWidth: 60,
                        name: ''
                    });
                    break;
                }
                default: {
                    result.push({
                        enableHiding: false,
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: camelPad(column),
                        width: '*'
                    });
                    break;
                }
            }
        }
        return result;
    }

    get blastHistoryColumns() {
        if (!localStorage.getItem('blastHistoryColumns')) {
            localStorage.setItem('blastHistoryColumns', JSON.stringify(DEFAULT_BLAST_HISTORY_COLUMNS));
        }
        let columns = JSON.parse(localStorage.getItem('blastHistoryColumns'));
        let defaultColumnsExists = true;
        for (let i = 0; i < DEFAULT_BLAST_HISTORY_COLUMNS.length; i++) {
            if (columns.map(c => c.toLowerCase()).indexOf(DEFAULT_BLAST_HISTORY_COLUMNS[i].toLowerCase()) === -1) {
                defaultColumnsExists = false;
                break;
            }
        }
        if (!defaultColumnsExists) {
            columns = DEFAULT_BLAST_HISTORY_COLUMNS.map(c => c);
            localStorage.setItem('blastHistoryColumns', JSON.stringify(columns || []));
        }
        return columns;
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
        }
        if (state === blastSearchState.SEARCHING) {
            duration = Date.now() - search.createdDate;
        } else {
            duration = search.endDate - search.createdDate;
        }
        return {
            id: search.id,
            title: search.title,
            currentState: state,
            submitted: search.createdDate,
            duration: duration,
            isInProcess: state === blastSearchState.SEARCHING
        };
    }
}
