export default class NgbBookmarksTableService {

    static instance(localDataService) {
        return new NgbBookmarksTableService(localDataService);
    }

    localDataService;

    constructor(localDataService) {
        Object.assign(this, {localDataService});
    }

    getBookmarksGridColumns() {

        const deleteCells = require('./ngbBookmarksTable_actions.tpl.html');
        const headerCells = require('./ngbBookmarksTable_header.tpl.html');

        const columnDefs = [
            {
                field: 'name',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Name',
                width: '*'
            },
            {
                field: 'chromosome.name',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Chromosome',
                width: '*'
            },
            {
                field: 'startIndex',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Start Position',
                width: '*',
            },
            {
                field: 'endIndex',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'End Position',
                width: '*',
            },
            {
                cellTemplate: deleteCells,
                enableSorting: false,
                field: 'id',
                headerCellTemplate: '<span></span>',
                maxWidth: 50,
                minWidth: 50,
                name: ''
            }
        ];


        return {columnDefs};
    }

    loadBookmarks() {
        return this.localDataService.getBookmarks();;
    }

    loadBookmark(bookmarkId) {
        return this.localDataService.getBookmark(bookmarkId);
    }

    deleteBookmark(bookmarkId) {
        return this.localDataService.deleteBookmark(bookmarkId);
    }
}