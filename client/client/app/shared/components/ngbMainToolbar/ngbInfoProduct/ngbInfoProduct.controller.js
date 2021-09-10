export default class ngbInfoProductController {
    static get UID () {
        return 'ngbInfoProductController';
    }
    constructor(projectContext) {
        this.projectContext = projectContext;
    }

    get docsUrl() {
        return this.projectContext.ngbDefaultSettings &&
            this.projectContext.ngbDefaultSettings.home &&
            this.projectContext.ngbDefaultSettings.home.docs;
    }
}
