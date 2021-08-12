export default class ngbMotifsPanelController{

    static get UID() {
        return 'ngbMotifsPanelController';
    }

    constructor($scope, dispatcher, ngbMotifsPanelService) {
        Object.assign(this, {$scope, dispatcher, ngbMotifsPanelService});
    }

    get isSearchInProgress () {
        return this.ngbMotifsPanelService.isSearchInProgress;
    }

    get isSearchFailure () {
        return this.ngbMotifsPanelService.isSearchFailure;
    }

    get errorMessageList () {
        return this.ngbMotifsPanelService.errorMessageList;
    }

    get isSearchResults () {
        return this.ngbMotifsPanelService.isSearchResults;
    }
}
