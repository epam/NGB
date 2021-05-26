import {camelPad} from '../../../shared/utils/String';

const DEFAULT_BLAST_HISTORY_COLUMNS = [
    'id', 'title', 'currentState', 'submitted', 'duration', 'actions'
];
const blastSearchState = {
    DONE: 'DONE',
    FAILURE: 'FAILURE',
    SEARCHING: 'SEARCHING'
};
const FIRST_PAGE = 1;
const PAGE_SIZE_HISTORY = 12;
const REFRESH_INTERVAL_SEC = 10;

export default class ngbBlastHistoryTableService {

    static instance(projectDataService) {
        return new ngbBlastHistoryTableService(projectDataService);
    }

    _blastHistory;
    _firstPageHistory = FIRST_PAGE;
    _lastPageHistory = FIRST_PAGE;
    _currentPageHistory = FIRST_PAGE;
    _historyPageError = null;

    constructor(projectDataService) {
        this.projectDataService = projectDataService;
    }

    get firstPageHistory() {
        return this._firstPageHistory;
    }

    set firstPageHistory(value) {
        this._firstPageHistory = value;
    }

    get lastPageHistory() {
        return this._lastPageHistory;
    }

    set lastPageHistory(value) {
        this._lastPageHistory = value;
    }

    get currentPageHistory() {
        return this._currentPageHistory;
    }

    set currentPageHistory(value) {
        this._currentPageHistory = value;
    }

    get historyPageSize() {
        return PAGE_SIZE_HISTORY;
    }

    get refreshInterval() {
        return REFRESH_INTERVAL_SEC*1000;
    }

    get historyPageError() {
        return this._historyPageError;
    }

    set blastHistoryColumns(columns) {
        localStorage.setItem('blastHistoryColumns', JSON.stringify(columns || []));
    }

    get blastHistory() {
        return this._blastHistory;
    }

    async updateSearchHistory() {
        this._blastHistory = await this.loadBlastHistory();
    }

    async deleteBlastSearch(id) {
        this.projectDataService.deleteBlastSearch(id).then(async () => {
            this._blastHistory = await this.loadBlastHistory();
        });
    }

    async cancelBlastSearch(id) {
        this.projectDataService.cancelBlastSearch(id).then(async () => {
            this._blastHistory = await this.loadBlastHistory();
        });
    }

    async clearSearchHistory() {
        this._blastHistory = await this.loadBlastHistory();
    }

    async loadBlastHistory() {
        const data = await this.projectDataService.getBlastHistoryLoad();
        if (data.error) {
            this._totalPagesCountHistory = 0;
            this._currentPageHistory = FIRST_PAGE;
            this._lastPageHistory = FIRST_PAGE;
            this._firstPageHistory = FIRST_PAGE;
            this._hasMoreHistory = true;
            this._historyPageError = data.message;
            // this.dispatcher.emit('blast:history:page:loading:finished');
            return [];
        } else {
            this._historyPageError = null;
        }
        if (data.totalPagesCount === 0) {
            data.totalPagesCount = undefined;
        }
        data.forEach(this._formatServerToClient);
        return data;
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
            case 1:
            case 2:
            case 3: {
                state = blastSearchState.SEARCHING;
                break;
            }
            case 5: {
                state = blastSearchState.FAILURE;
                break;
            }
            case 6: {
                state = blastSearchState.DONE;
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
