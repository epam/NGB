const PAGE_DEEPNESS = 3;

export default class ngbTargetsTablePaginateController {

    static get UID() {
        return 'ngbTargetsTablePaginateController';
    }

    constructor(dispatcher, ngbTargetsTableService) {
        Object.assign(this, {dispatcher, ngbTargetsTableService});
        this.pages = this.getPages();
    }

    get totalPages () {
        return this.ngbTargetsTableService.totalCount;
    }

    get currentPage () {
        return this.ngbTargetsTableService.currentPage;
    }
    set currentPage (value) {
        this.ngbTargetsTableService.currentPage = value;
    }

    setPage(page, loadData) {
        if (this.totalPages === undefined || page < 1 || page > this.totalPages) {
            return;
        }
        if (loadData){
            this.dispatcher.emit('targets:pagination:changed', page);
        }
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
