export default class ngbCoverageTableActionsController {

    static get UID() {
        return 'ngbCoverageTableActionsController';
    }

    get displayFilters() {
        return this.ngbCoveragePanelService.displayFilters;
    }
    set displayFilters(value) {
        this.ngbCoveragePanelService.displayFilters = value;
    }

    constructor($scope, dispatcher, ngbCoveragePanelService) {
        Object.assign(this, {$scope, dispatcher, ngbCoveragePanelService});
    }

    onClickRestoreView() {
        this.dispatcher.emitSimpleEvent('coverage:table:restore');
    }
}
