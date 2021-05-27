import baseController from '../../../shared/baseController';

export default class ngbBlastSearchPanelPaginate extends baseController {

    static get UID() {
        return 'ngbBlastSearchPanelPaginate';
    }

    constructor(dispatcher, $scope, $timeout) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
        });
        this.initEvents();
    }

    events = {
        'blast:loading:finished': ([totalPages, currentPage]) => {
            this.refresh(totalPages, currentPage);
        },
        'pageBlast:scroll': (page) => {
            this.setPage(page);
        },
    };

    $onChanges(changes) {
        let needRefresh = false;
        if (changes.totalPages && (changes.totalPages.previousValue !== changes.totalPages.currentValue)) {
            needRefresh = true;
        }
        if (changes.currentPage && (changes.currentPage.previousValue !== changes.currentPage.currentValue)) {
            needRefresh = true;
        }
        if (needRefresh) {
            this.refresh(changes.totalPages.currentValue, this.currentPage);
        }
    }

    refresh(totalPages, currentPage) {
        this.totalPages = totalPages;
        this.pages = this.getPages();
        this.setPage(currentPage);
    }

    setPage(page) {
        if (this.totalPages === undefined || page < 1 || page > this.totalPages) {
            return;
        }

        this.currentPage = page;
        this.changePage({page: this.currentPage});
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
