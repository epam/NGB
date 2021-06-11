export default class ngbBlastSearchAlignmentList {

    static get UID() {
        return 'ngbBlastSearchAlignmentList';
    }

    searchResult = {};
    isProgressShown = true;

    constructor(ngbBlastSearchService, $timeout) {
        Object.assign(this, {
            ngbBlastSearchService, $timeout
        });
        this.initialize();
    }

    initialize() {
        this.searchResult = this.ngbBlastSearchService.popCurrentAlignmentObject();
        this.search = this.ngbBlastSearchService.cutCurrentResult;
        this.$timeout(() => this.isProgressShown = false, 1000);
    }
}
