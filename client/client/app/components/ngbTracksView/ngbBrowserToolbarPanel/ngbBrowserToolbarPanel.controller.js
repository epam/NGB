export default class ngbBrowserToolbarPanelController {

    static get UID() {
        return 'ngbBrowserToolbarPanelController';
    }

    showScreenShot = true;
    projectContext;

    constructor(dispatcher, projectContext) {
        Object.assign(this, {dispatcher, projectContext});
    }

    $onInit() {
        const screenShotVisibility = this.projectContext.screenShotVisibility;
        this.showScreenShot = screenShotVisibility;
    }

    zoomIn() {
        this.zoomManager.zoomIn();
    }

    zoomOut() {
        this.zoomManager.zoomOut();
    }

    get canZoom() {
        return this.zoomManager && this.zoomManager.canZoom;
    }

    saveBrowserView() {
        this.bookmarkCamera.saveBrowserView();
    }
} 
