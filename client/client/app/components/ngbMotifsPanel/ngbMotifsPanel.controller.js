export default class ngbMotifsPanelController{

    static get UID() {
        return 'ngbMotifsPanelController';
    }

    constructor($scope, ngbMotifsPanelService) {
        Object.assign(this, {$scope, ngbMotifsPanelService});
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

    get isShowParamsTable () {
        return this.ngbMotifsPanelService.isShowParamsTable;
    }
}
