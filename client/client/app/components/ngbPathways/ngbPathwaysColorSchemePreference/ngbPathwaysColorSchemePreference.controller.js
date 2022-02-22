import baseController from '../../../shared/baseController';

export default class ngbPathwaysColorSchemePreferenceController extends baseController {
    dispatcher;
    isProgressShown = true;
    errorMessageList = [];
    events = {
    };

    constructor($scope, $timeout, dispatcher,
        ngbPathwaysColorSchemePreferenceConstants) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbPathwaysColorSchemePreferenceConstants
        });

        this.scheme = this.scheme || {};
        this.constants = ngbPathwaysColorSchemePreferenceConstants;

        this.initEvents();
    }

    static get UID() {
        return 'ngbPathwaysColorSchemePreferenceController';
    }
}
