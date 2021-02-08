import  baseController from '../../../shared/baseController';

const Math = window.Math;

export default class ngbVariantsTablePaginateController extends baseController {

    static get UID() {
        return 'ngbVariantsTablePaginateController';
    }

    projectContext;

    constructor(dispatcher, projectContext, $scope, $timeout) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext
        });
       
        this.totalPages = this.projectContext.totalPagesCountVariations;
        this.currentPage = this.projectContext.currentPageVariations;
        this.pages = this.getPages();

        this.initEvents();
    }

    refresh() {
        this.totalPages = this.projectContext.totalPagesCountVariations;
        this.setPage(this.projectContext.currentPageVariations);
    }

    events = {
        'pageVariations:scroll': (page) => {
            this.setPage(page);
        },
        'variants:loading:finished': this.refresh.bind(this),
        'activeVariants': this.refresh.bind(this),
        'reference:change': this.refresh.bind(this)
    };

    setPage(page, loadData)
    {
        if (this.totalPages === undefined || page < 1 || page > this.totalPages) {
            return;
        }

        if(loadData){
            this.dispatcher.emit('pageVariations:change', page);
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
