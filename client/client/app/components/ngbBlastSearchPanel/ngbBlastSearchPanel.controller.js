export default class ngbBlastSearchPanelController {

    static get UID() {
        return 'ngbBlastSearchPanelController';
    }
    projectContext;
    constructor($scope, dispatcher, ngbBlastSearchService) {
        Object.assign(this, {
            $scope,
            dispatcher,
            ngbBlastSearchService,
        })
    }

    handleOpenGenomeView() {
        const data = this.ngbBlastSearchService.generateBlastSearchResults();
        this.dispatcher.emitSimpleEvent('blast:whole:genome:view', { data });
    }
}
