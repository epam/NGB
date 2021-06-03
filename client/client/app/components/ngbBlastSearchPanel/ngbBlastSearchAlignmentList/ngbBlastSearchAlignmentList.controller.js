export default class ngbBlastSearchAlignmentList {

    static get UID() {
        return 'ngbBlastSearchAlignmentList';
    }

    searchResult = {};
    isProgressShown = true;

    constructor(ngbBlastSearchService) {
        Object.assign(this, {
            ngbBlastSearchService
        });
        this.initialize();
    }

    initialize() {
        this.searchResult = this.ngbBlastSearchService.popCurrentAlignmentObject();
        this.isProgressShown = false;
    }
}
