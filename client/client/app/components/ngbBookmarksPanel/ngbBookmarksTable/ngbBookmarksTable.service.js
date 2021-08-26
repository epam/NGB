const BOOKMARKS_COLUMNS = [
    'name', 'description', 'reference', 'chromosome', 'startIndex', 'endIndex', 'owner'
];

const BOOKMARKS_COLUMN_TITLES = {
    'chromosome': 'Chr',
    'startIndex': 'Start',
    'endIndex': 'End',
};

const blockFilterBookmarksTimeout = 500;

export default class ngbBookmarksTableService {

    _blockFilterBookmarks;

    constructor(dispatcher, localDataService) {
        Object.assign(this, {
            dispatcher,
            localDataService
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

    static instance(dispatcher, localDataService) {
        return new ngbBookmarksTableService(dispatcher, localDataService);
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

    loadBookmarks() {
        return this.localDataService.getBookmarks();
    }

    loadBookmark(bookmarksId) {
        return this.localDataService.getBookmarks(bookmarksId);
    }

    deleteBookmarks(bookmarksId) {
        return this.localDataService.deleteBookmarks(bookmarksId);
    }
}
