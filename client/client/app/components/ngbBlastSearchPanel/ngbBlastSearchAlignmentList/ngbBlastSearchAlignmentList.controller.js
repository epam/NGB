import angular from 'angular';

export default class ngbBlastSearchAlignmentList {

    static get UID() {
        return 'ngbBlastSearchAlignmentList';
    }

    searchResult = {};
    windowElm = {};
    isProgressShown = true;

    constructor(ngbBlastSearchService, $timeout, $window) {
        Object.assign(this, {
            ngbBlastSearchService, $timeout
        });
        this.windowElm = angular.element($window);
        this.initialize();
    }

    initialize() {
        this.searchResult = this.ngbBlastSearchService.popCurrentAlignmentObject();
        this.search = this.ngbBlastSearchService.cutCurrentResult;
        this.$timeout(() => {
            this.windowElm.resize();
            this.$timeout(() => {
                this.windowElm.resize();
                this.$timeout(() => this.isProgressShown = false, 0);
            }, 0);
        }, 0);
    }
}
