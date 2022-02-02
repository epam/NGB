export default class ngbCytoscapeToolbarPanelController {

    constructor() {
    }

    static get UID() {
        return 'ngbCytoscapeToolbarPanelController';
    }

    zoomIn() {
        this.actionsManager.zoomIn();
    }

    zoomOut() {
        this.actionsManager.zoomOut();
    }

    restoreDefault() {
        this.actionsManager.restoreDefault();
    }
}
