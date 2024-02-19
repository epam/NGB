const PAGE_DEEPNESS = 3;

export default class ngbPaginationController {


    static get UID() {
        return 'ngbPaginationController';
    }

    constructor($scope, $timeout) {
        Object.assign(this, {$scope, $timeout});
        $scope.$watch('$ctrl.currentPage', this.refresh.bind(this));
        $scope.$watch('$ctrl.totalPages', this.refresh.bind(this));
        this.refresh();
    }

    $onInit() {
        this.refresh();
    }

    refresh() {
        this.pages = this.getPages();
        this.$timeout(() => this.$scope.$apply());
    }

    setPage(page) {
        if (typeof this.onChangePage === 'function') {
            this.onChangePage(page);
        }
    }

    getPages() {
        const {totalPages: total = 0, currentPage: current = 0} = this;
        const totalPages = Number.isNaN(Number(total)) ? 0 : Number(total);
        const currentPage = Number.isNaN(Number(current)) ? 0 : Number(current);
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
