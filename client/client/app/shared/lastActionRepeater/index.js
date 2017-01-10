export default class LastActionRepeater {

    lastOperation;

    static instance(dispatcher) {
        return new LastActionRepeater(dispatcher);
    }

    constructor(dispatcher) {
        this.dispatcher = dispatcher;
        this.dispatcher.on('hotkeyPressed', (event) => this._handleEvent(event));
    }

    _handleEvent(event) {
        if (event === 'general>repeatLastOperation' && this.lastOperation) {
            this.dispatcher.emitGlobalEvent('hotkeyPressed', this.lastOperation);
            return;
        }
        this.lastOperation = event;
    }

    rememberAction(actionName) {
        if (actionName && typeof actionName === 'string') {
            this.lastOperation = actionName;
        }
    }

}