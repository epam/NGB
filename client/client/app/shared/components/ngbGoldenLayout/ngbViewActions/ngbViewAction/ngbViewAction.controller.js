export default class ngbViewActionController {
    static get UID() {
        return 'ngbViewActionController';
    }

    dispatcher;

    constructor(dispatcher) {
        this.dispatcher = dispatcher;
    }

    doAction() {
        this.dispatcher.emitGlobalEvent(this.event);
    }
}