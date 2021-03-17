export default class ngbBlastSearchPanelController {

    static get UID() {
        return 'ngbBlastSearchPanelController';
    }

    constructor($scope, dispatcher, projectContext) {
        Object.assign(this, {
            $scope,
            dispatcher,
            projectContext
        })
    }

    isProgressShown = true;

    handleOpenGenomeView() {
        this.dispatcher.emitSimpleEvent('blast:whole:genome:view', {});
    }
}