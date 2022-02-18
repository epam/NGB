export default class ngbCoverageTableActionsController {


    constructor($scope, dispatcher) {
        Object.assign(this, {$scope, dispatcher});
    }

    static get UID() {
        return 'ngbCoverageTableActionsController';
    }

    onClickRestoreView() {
        this.dispatcher.emitSimpleEvent('coverage:table:restore');
    }
}
