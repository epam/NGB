/**
 * Class representing a event
 */
export class DispatcherEvent {
    /**
     * Create event
     * @param {string} event - name of event
     * @param {object} state - currnet state of browser
     */
    constructor(event, state) {
        this._eventName = event;
        this._stateObject = state;
    }

    /**
     *
     * @returns {string|*}
     */
    get name() {
        return this._eventName;
    }

    /**
     * 
     * @returns {Object|*}
     */
    get state() {
        return this._stateObject;
    }
}