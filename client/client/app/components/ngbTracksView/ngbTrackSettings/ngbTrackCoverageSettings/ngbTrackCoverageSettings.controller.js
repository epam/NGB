export default class ngbTrackCoverageSettingsController {

    static get UID() {
        return 'ngbTrackCoverageSettingsController';
    }

    projectContext;
    dispatcher;

    constructor($scope, projectContext, dispatcher) {
        this.projectContext = projectContext;
        this.dispatcher = dispatcher;
    }
}
