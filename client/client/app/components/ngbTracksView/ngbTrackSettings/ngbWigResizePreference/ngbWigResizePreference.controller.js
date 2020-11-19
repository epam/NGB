import baseController from '../../../../shared/baseController';

export default class ngbWigResizePreferenceController extends baseController {
    static get UID() {
        return 'ngbWigResizePreferenceController';
    }

    constructor($scope, projectContext, dispatcher) {
        super($scope);

        Object.assign(this, {
            $scope,
            dispatcher,
            projectContext,
        });
    }
}
