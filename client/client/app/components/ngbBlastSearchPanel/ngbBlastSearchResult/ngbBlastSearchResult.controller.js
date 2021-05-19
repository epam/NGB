import baseController from '../../../shared/baseController';

export default class ngbBlastSearchPanelPaginate extends baseController {

    static get UID() {
        return 'ngbBlastSearchPanelPaginate';
    }

    readSequence = '';
    searchResult = {};

    constructor(dispatcher, $scope, $timeout, ngbBlastSearchService) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbBlastSearchService
        });

        this.initEvents();
        this.initialize();
    }

    initialize() {
        this.searchResult = this.ngbBlastSearchService.currentSearchResult;
    }

    editSearch(event) {
        this.ngbBlastSearchService.currentSearchId = this.ngbBlastSearchService.currentResultId;
        this.changeTab({tab: 'SEARCH'});
        event.stopImmediatePropagation();
        return false;
    }

    downloadResults(event) {
        event.stopImmediatePropagation();
        return false;
    }

    browseGenomeView(event) {
        event.stopImmediatePropagation();
        return false;
    }
}
