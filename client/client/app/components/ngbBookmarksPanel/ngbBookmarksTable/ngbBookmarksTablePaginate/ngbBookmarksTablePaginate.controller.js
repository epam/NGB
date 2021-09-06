import  baseController from '../../../../shared/baseController';

const Math = window.Math;

export default class ngbBookmarksTablePaginateController extends baseController {

    events = {
        'bookmarks:loaded': this.refresh.bind(this)
    };

    constructor(dispatcher, ngbBookmarksTableService, $scope, $timeout) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbBookmarksTableService
        });
       
        this.totalPages = this.ngbBookmarksTableService.totalPages;
        this.currentPage = this.ngbBookmarksTableService.currentPage;
        this.pages = this.getPages();
        this.setPage = this.ngbBookmarksTableService.changePage.bind(ngbBookmarksTableService);

        this.initEvents();
    }

    static get UID() {
        return 'ngbBookmarksTablePaginateController';
    }

    refresh() {
        this.totalPages = this.ngbBookmarksTableService.totalPages;
        this.pages = this.getPages();
        this.setPage(this.ngbBookmarksTableService.currentPage);
    }

    getPages() {
        const totalPages = this.totalPages;
        const currentPage = this.currentPage;
        if (totalPages === undefined || currentPage === undefined) {
            return [];
        }

        let minimumPage = Math.max(1, currentPage - 3);
        let maximumPage = Math.min(totalPages, currentPage + 3);
        minimumPage = Math.max(1, Math.min(minimumPage, maximumPage - 6));
        maximumPage = Math.min(Math.max(maximumPage, minimumPage + 6), totalPages);

        const pages = [];
        for (let i = minimumPage; i <= maximumPage; i++) {
            if (i === minimumPage && minimumPage > 1) {
                pages.push({
                    isFirst: true,
                    isLast: false,
                    value: 1
                });
            } else if (i === maximumPage && maximumPage < totalPages) {
                pages.push({
                    isFirst: false,
                    isLast: true,
                    value: totalPages
                });
            } else {
                pages.push({
                    isFirst: false,
                    isLast: false,
                    value: i
                });
            }
        }
        return pages;
    }

}
