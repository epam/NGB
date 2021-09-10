import  baseController from '../../../../shared/baseController';

export default class ngbBookmarksTableColumnController extends baseController {

    displayBookmarksFilter;
    events = {
        'display:bookmarks:filter' : ::this.updateDisplayBookmarksFilterValue
    };

    constructor(dispatcher, ngbBookmarksTableService, $scope, $timeout) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbBookmarksTableService
        });

        this.displayBookmarksFilter = this.ngbBookmarksTableService.displayBookmarksFilter;

        this.initEvents();
    }

    static get UID() {
        return 'ngbBookmarksTableColumnController';
    }

    updateDisplayBookmarksFilterValue() {
        this.displayBookmarksFilter = this.ngbBookmarksTableService.displayBookmarksFilter;
    }

    onDisplayBookmarksFilterChange() {
        this.ngbBookmarksTableService.setDisplayBookmarksFilter(this.displayBookmarksFilter, false);
    }

    onBookmarksRestoreViewClick() {
        this.dispatcher.emit('bookmarks:restore');
    }
}
