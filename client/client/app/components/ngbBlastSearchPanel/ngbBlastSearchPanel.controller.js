export default class ngbBlastSearchPanelController {

    static get UID() {
        return 'ngbBlastSearchPanelController';
    }

    constructor($scope, dispatcher) {
        Object.assign(this, {
            $scope,
            dispatcher,
        })
    }

    isProgressShown = true;

    handleOpenGenomeView() {
        this.dispatcher.emitSimpleEvent('blast:whole:genome:view', {});
    }
}
