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
const PAGE_SIZE = 15;

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
                        cellTooltip: true,
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
                case 'reference': {
                    columnSettings = {
                        enableHiding: false,
                        enableFiltering: true,
                        enableSorting: true,
                        field: 'reference.name',
                        cellTooltip: true,
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
                        enableFiltering: false,
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
                        cellTooltip: true,
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

    getRequestFilter() {
        return item => {
            let result = true;
            if (this.bookmarksFilter.name) {
                result &= item.name.toLowerCase().includes(this.bookmarksFilter.name.toLowerCase());
            }
            if (this.bookmarksFilter.description) {
                result &= item.description && item.description.toLowerCase().includes(this.bookmarksFilter.description.toLowerCase());
            }
            if (this.bookmarksFilter.chromosome && this.bookmarksFilter.chromosome.length) {
                result &= item.chromosome && this.bookmarksFilter.chromosome.includes(item.chromosome.id);
            }
            if (this.bookmarksFilter.reference && this.bookmarksFilter.reference.length) {
                result &= item.reference && this.bookmarksFilter.reference.includes(item.reference.id);
            }
            if (this.bookmarksFilter.startIndex) {
                result &= item.startIndex >= this.bookmarksFilter.startIndex;
            }
            if (this.bookmarksFilter.endIndex) {
                result &= item.endIndex <= this.bookmarksFilter.endIndex;
            }
            if (this.bookmarksFilter.owner) {
                result &= item.owner && item.owner.toLowerCase().includes(this.bookmarksFilter.owner.toLowerCase());
            }
            return !!result;
        };
    }

    async loadBookmarks() {
        const filterFn = this.getRequestFilter();
        const data = [];
        const serverData = await this.bookmarkDataService.loadBookmarks();
        const localData = this.localDataService.getBookmarks();
        localData.forEach(value => {
            data.push(this._formatLocalToClient(value));
        });
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
        const filteredData = data.filter(filterFn);
        this._totalPages = Math.ceil(filteredData.length / this.pageSize);
        if (this.currentPage < this._totalPages) {
            this.currentPage = FIRST_PAGE;
        }
        this.dispatcher.emit('bookmarks:loaded');
        return filteredData || [];
    }

    deleteBookmark(bookmarksId, isLocal) {
        if (isLocal) {
            return new Promise(resolve => {
                this.localDataService.deleteBookmark(bookmarksId);
                resolve();
            });
        } else {
            return this.bookmarkDataService.deleteBookmark(bookmarksId);
        }
    }

    _formatServerToClient(result) {
        return {
            id: result.id,
            owner: result.owner,
            isLocal: false,
            ...JSON.parse(result.sessionValue)
        };
    }

    _formatLocalToClient(result) {
        result.isLocal = true;
        return result;
    }
}
