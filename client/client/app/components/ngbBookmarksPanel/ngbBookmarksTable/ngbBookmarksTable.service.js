import ClientPaginationService from '../../../shared/services/clientPaginationService';

const BOOKMARKS_COLUMNS = [
    'name', 'description', 'reference', 'chromosome', 'startIndex', 'endIndex', 'owner', 'info'
];

const BOOKMARKS_COLUMN_TITLES = {
    'chromosome': 'Chr',
    'startIndex': 'Start',
    'endIndex': 'End',
};

const blockFilterBookmarksTimeout = 500;
const FIRST_PAGE = 1;
const PAGE_SIZE = 2;

export default class ngbBookmarksTableService extends ClientPaginationService {

    _blockFilterBookmarks;

    constructor(dispatcher, localDataService, bookmarkDataService) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'bookmarks:page:change');
        Object.assign(this, {
            dispatcher,
            localDataService,
            bookmarkDataService
        });
        this.clearBookmarksFilter();
    }

    get bookmarksTableColumns() {
        return BOOKMARKS_COLUMNS;
    }

    _bookmarksFilter = {};

    get bookmarksFilter() {
        return this._bookmarksFilter;
    }

    _displayBookmarksFilter;

    get displayBookmarksFilter() {
        return !!this._displayBookmarksFilter;
    }

    _pageError = null;

    get pageError() {
        return this._pageError;
    }

    static instance(dispatcher, localDataService, bookmarkDataService) {
        return new ngbBookmarksTableService(dispatcher, localDataService, bookmarkDataService);
    }

    getBookmarksColumnTitle(column) {
        return BOOKMARKS_COLUMN_TITLES[column] || column;
    }

    setDisplayBookmarksFilter(value, updateScope = true) {
        if (value !== this._displayBookmarksFilter) {
            this._displayBookmarksFilter = value;
            this.dispatcher.emitSimpleEvent('display:bookmarks:filter', updateScope);
        }
    }

    getBookmarksGridColumns() {
        const headerCells = require('./ngbBookmarksTable_header.tpl.html');
        const columnsList = this.bookmarksTableColumns;
        const result = [];

        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            switch (column) {
                case 'chromosome': {
                    columnSettings = {
                        enableHiding: false,
                        enableFiltering: true,
                        enableSorting: true,
                        field: 'chromosome.name',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.getBookmarksColumnTitle(column),
                        filterApplied: () => this.bookmarksFieldIsFiltered(column),
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.clearBookmarksFieldFilter(column),
                                shown: () => this.bookmarksFieldIsFiltered(column)
                            }
                        ],
                        width: '*'
                    };
                    break;
                }
                case 'info': {
                    columnSettings = {
                        cellTemplate: `
                                    <md-button
                                            class="md-accent md-flat bookmark-delete-button"
                                            type="button"
                                            aria-label="Clear history"
                                            ng-click="grid.appScope.$ctrl.onRemove(row, $event)"
                                    >
                                        <ng-md-icon icon="delete"></ng-md-icon>
                                    </md-button>`,
                        field: 'info',
                        headerCellTemplate: headerCells,
                        enableSorting: false,
                        enableColumnMenu: false,
                        minWidth: 20,
                        maxWidth: 40,
                        name: '',
                        width: '*'
                    };
                    break;
                }
                default: {
                    columnSettings = {
                        enableHiding: false,
                        enableFiltering: true,
                        enableSorting: true,
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.getBookmarksColumnTitle(column),
                        filterApplied: () => this.bookmarksFieldIsFiltered(column),
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.clearBookmarksFieldFilter(column),
                                shown: () => this.bookmarksFieldIsFiltered(column)
                            }
                        ],
                        width: '*'
                    };
                    break;
                }
            }
            result.push(columnSettings);
        }

        return result;
    }

    bookmarksFieldIsFiltered(fieldName) {
        return this.bookmarksFilter[fieldName] !== undefined;
    }

    clearBookmarksFieldFilter(fieldName) {
        this.bookmarksFilter[fieldName] = undefined;
        this.dispatcher.emit('bookmarks:refresh');
    }

    clearBookmarksFilter() {
        if (this._blockFilterBookmarks) {
            clearTimeout(this._blockFilterBookmarks);
            this._blockFilterBookmarks = null;
        }
        this._hasMoreBokkmark = true;
        this._bookmarksFilter = {};
        this.dispatcher.emit('bookmarks:refresh');
        this._blockFilterBookmarks = setTimeout(() => {
            this._blockFilterBookmarks = null;
        }, blockFilterBookmarksTimeout);
    }

    canScheduleFilterBookmarks() {
        return !this._blockFilterBookmarks;
    }

    scheduleFilterBookmarks() {
        if (this._blockFilterBookmarks) {
            return;
        }
        this.dispatcher.emit('bookmarks:refresh');
    }

    async loadBookmarks() {
        const filter = {

        };
        const serverData = await this.bookmarkDataService.loadBookmarks(filter);
        const data = this.localDataService.getBookmarks();
        if (serverData.error) {
            this._totalPages = 0;
            this.currentPage = FIRST_PAGE;
            this._firstPage = FIRST_PAGE;
            this._pageError = data.message;
            return [];
        } else {
            this._pageError = null;
            serverData.forEach(value => {
                data.push(this._formatServerToClient(value));
            });
        }
        this._totalPages = Math.ceil(data.length / this.pageSize);
        this.dispatcher.emit('bookmarks:loaded');
        return data || [];
    }

    deleteBookmarks(bookmarksId) {
        return this.localDataService.deleteBookmarks(bookmarksId);
    }

    _formatServerToClient(result) {
        return result;
    }
}
