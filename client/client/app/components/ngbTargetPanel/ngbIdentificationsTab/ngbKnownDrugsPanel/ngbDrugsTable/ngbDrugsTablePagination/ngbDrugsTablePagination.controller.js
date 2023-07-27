const PAGE_DEEPNESS = 3;

export default class ngbDrugsTablePaginationController {


    static get UID() {
        return 'ngbDrugsTablePaginationController';
    }

    constructor($scope, $timeout, dispatcher, ngbDrugsTableService, ) {
        Object.assign(this, {$scope, $timeout, ngbDrugsTableService});
        this.pages = this.getPages();
        dispatcher.on('target:identification:drugs:results:updated', this.refresh.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:drugs:results:updated', this.refresh.bind(this));
        });
    }

    get totalPages() {
        return this.ngbDrugsTableService.totalPages;
    }
    get currentPage() {
        return this.ngbDrugsTableService.currentPage;
    }
    set currentPage(value) {
        this.ngbDrugsTableService.currentPage = value;
    }

    refresh() {
        this.pages = this.getPages();
        this.$timeout(() => this.$scope.$apply());
    }

    async setPage(page) {
        if (this.totalPages === undefined || page < 1 || page > this.totalPages) {
            return;
        }
        await this.onChangePage({page: page});
        this.currentPage = page;
        this.pages = this.getPages();
    }

    getPages() {
        const totalPages = this.totalPages;
        const currentPage = this.currentPage;
        if (totalPages === undefined || currentPage === undefined) {
            return [];
        }

        let minimumPage = Math.max(1, currentPage - PAGE_DEEPNESS);
        let maximumPage = Math.min(totalPages, currentPage + PAGE_DEEPNESS);
        minimumPage = Math.max(1, Math.min(minimumPage, maximumPage - (PAGE_DEEPNESS * 2)));
        maximumPage = Math.min(Math.max(maximumPage, minimumPage + (PAGE_DEEPNESS * 2)), totalPages);

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
