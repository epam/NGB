export default class ngbSearchController {

    /**
     * @returns {string}
     * @constructor
     */
    static get UID() {
        return 'ngbSearchController';
    }

    /**
     *
     * @param {ngbSearchMessage}message
     */
    /** @ngInject */
    constructor(ngbSearchMessage, dispatcher) {
        this.dispatcher = dispatcher;
        this._messages = ngbSearchMessage;
        this.INIT();
    }

    /**
     * Init function
     */
    INIT() {
        this.Placeholder = this._messages.Placeholder;
    }

    changeSearch() {
        this.dispatcher.emitGlobalEvent('Search:change', {'Search': this.Search});
    }
}




