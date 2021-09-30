const EventEmitter = require('events');
import {GlobalEvent} from './events/global-event';
import deepExtend from 'deep-extend';
import {isEquivalent} from '../utils/Object';

let _instance;
/**
 * Dispatcher
 * @extends EventEmitter (https://nodejs.org/api/events.html#events_emitter_on_eventname_listener)
 *
 */
export class dispatcher extends EventEmitter {

    constructor() {
        super();
        this.setMaxListeners(Infinity);
        _instance = this;
    }

    /**
     * @param {DispatcherEvent} event
     */
    //deprecated, use emitGlobalEvent
    emitEvent(event, hub = true) {
        this._emitEventGeneral(event, hub);
    }

    emitEventHub(event) {
        this._preventDuplicate(event) && this._emitEventGeneral(event);
    }

    emitGlobalEvent(name, object, hub = true) {
        const event = new GlobalEvent(name, object);
        this._emitEventGeneral(event, hub);
    }

    emitSimpleEvent(name, object) {
        this.emit(name, object);
    }

    _emitEventGeneral(event, hub = false) {
        const state = event.state;
        this.eventsState = deepExtend((this.eventsState || {}), {[event.name]: state});

        if (!this.listenerCount()) {

            this.emit('log', event);
            this.emit(event.name, state);

            hub && this.emit('eventHub', event);
        }
    }


    emitError(message) {
        _instance.emit('error', {source: this.constructor.name, message: message});
    }

    emitServiceError(obj) {
        const message = `${obj.status}: ${obj.statusText} ${obj.config.url}`;
        _instance.emit('error', {source: 'Client Data Service', message: message});
    }

    eventsCollection() {
        return Object.keys(this._events);
    }

    getLastEvents() {
        return Object.keys(this.eventsState).map((key) => ({
            name: key,
            state: this.eventsState[key]
        }));
    }

    _preventDuplicate(event) {
        const state = event.state;
        return !(this.eventsState && this.eventsState[event.name] && isEquivalent(this.eventsState[event.name], state));
    }

    /**
     *
     * @returns {Dispatcher}
     */
    static instance() {
        return new dispatcher();
    }
}
