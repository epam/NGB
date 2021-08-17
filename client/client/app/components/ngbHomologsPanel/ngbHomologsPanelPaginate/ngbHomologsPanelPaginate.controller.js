const PAGE_DEEPNESS = 3;
export default class ngbHomologsPanelPaginate {

    constructor() {
    }

    static get UID() {
        return 'ngbHomologsPanelPaginate';
    }

    $onChanges(changes) {
        if (!!changes.totalPages && (changes.totalPages.previousValue !== changes.totalPages.currentValue)) {
            this.setTotalPages(changes.totalPages.currentValue);
        }
        if (!!changes.currentPage && (changes.currentPage.previousValue !== changes.currentPage.currentValue)) {
            this.setPage(changes.currentPage.currentValue);
        }

    }

    setTotalPages(totalPages) {
        this.totalPages = totalPages;
        this.pages = this.getPages();
    }

    setPage(page) {
        this.currentPage = page;
        this.pages = this.getPages();
        this.changePage({page: this.currentPage});
    }

    getPages() {
        const totalPages = this.totalPages;
        const currentPage = this.currentPage;
        if (totalPages === undefined || currentPage === undefined) {
            return [];
        }

        let minimumPage = Math.max(1, currentPage - PAGE_DEEPNESS);
        let maximumPage = Math.min(totalPages, currentPage + PAGE_DEEPNESS);
        minimumPage = Math.max(1, Math.min(minimumPage, maximumPage - PAGE_DEEPNESS*2));
        maximumPage = Math.min(Math.max(maximumPage, minimumPage + PAGE_DEEPNESS*2), totalPages);

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
