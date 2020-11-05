export default class ngbWigColorPreferenceController {

    static get UID() {
        return 'ngbWigColorPreferenceController';
    }

    projectContext;
    dispatcher;

    constructor($scope, projectContext, dispatcher) {
        this.projectContext = projectContext;
        this.dispatcher = dispatcher;
    }
}
