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
}
