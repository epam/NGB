import baseController from '../../../../shared/baseController';

export default class ngbTrackResizePreferenceController extends baseController {
    static get UID() {
        return 'ngbTrackResizePreferenceController';
    }

    constructor($scope, projectContext, dispatcher) {
        super($scope);

        Object.assign(this, {
            $scope,
            dispatcher,
            projectContext,
        });

        const maxMessage =
            this.maxHeight === Infinity
                ? ''
                : `and less than or equal ${this.maxHeight}px.`;

        this.message = `Value must be greater than or equal ${this.minHeight}px ${maxMessage}`;
    }
}
