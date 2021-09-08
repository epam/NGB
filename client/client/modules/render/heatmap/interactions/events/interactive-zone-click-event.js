import InteractiveZoneEvent from './interactive-zone-event';
import eventTypes from './event-types';

/**
 * @class InteractiveZoneClickEvent
 * @extends InteractiveZoneEvent
 */
export default class InteractiveZoneClickEvent extends InteractiveZoneEvent {
    /**
     *
     * @param {MouseEvent} event
     * @param {HeatmapViewport} viewport
     * @param {boolean} [doubleClick = false] double-click flag
     */
    constructor(event, viewport, doubleClick = false) {
        super(event, viewport);
        this.name = eventTypes.click;
        /**
         * Double click flag
         * @type {boolean}
         */
        this.doubleClick = doubleClick;
    }
}
