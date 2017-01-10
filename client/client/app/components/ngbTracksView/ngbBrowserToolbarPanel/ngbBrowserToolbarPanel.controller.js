export default class ngbBrowserToolbarPanelController {

    static get UID() {
        return 'ngbBrowserToolbarPanelController';
    }

    showBookmark = true;
    showScreenShot = true;
    projectContext;

    constructor(dispatcher, projectContext) {
        Object.assign(this, {dispatcher, projectContext});
    }

    $onInit() {
        const bookmarkVisibility = this.projectContext.bookmarkVisibility;
        const screenShotVisibility = this.projectContext.screenShotVisibility;
        this.showBookmark = bookmarkVisibility;
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
