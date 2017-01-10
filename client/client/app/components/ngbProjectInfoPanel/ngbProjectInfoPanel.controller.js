export default class ngbProjectInfoPanelController {
    /**
     * @returns {string}
     */
    static get UID() {
        return 'ngbProjectInfoPanelController';
    }

    projectContext;

    /**
     * @constructor
     */
    /** @ngInject */
    constructor(projectContext) {
        this.projectContext = projectContext;
    }

    get containsVcfFiles() {
        return this.projectContext.containsVcfFiles;
    }
}