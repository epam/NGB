export default class ngbTrackActionsController {

    static get UID() {
        return 'ngbTrackActionsController';
    }

    constructor($scope) {
    }

    handle(item) {
        item.handleClick();
        if (this.onHandle) {
            this.onHandle(this.trackController);
        }
    }

    linkEnabled(link) {
        if (!link || link.enabled === undefined) {
            return true;
        }
        if (typeof link.enabled === 'function') {
            return link.enabled();
        }
        return link.enabled;
    }
}
