import InteractiveZoneEvent from './interactive-zone-event';
import eventTypes from './event-types';

const SCROLL_STEP_PIXELS = 10;

/**
 * @class InteractiveZoneScrollEvent
 * @extends InteractiveZoneEvent
 */
export default class InteractiveZoneScrollEvent extends InteractiveZoneEvent {
    /**
     *
     * @param {MouseEvent} event
     * @param {HeatmapViewport} viewport
     * @param {Object} [options] custom options
     */
    constructor(event, viewport, options = {}) {
        super(event, viewport, options);
        this.name = eventTypes.scroll;
        const {
            deltaX: _deltaX,
            deltaY: _deltaY,
            deltaFactor
        } = event;
        const moveByRange = SCROLL_STEP_PIXELS / viewport.scale.tickSize;
        /**
         * Scroll x delta
         * @type {number}
         */
        this.deltaX = Math.sign(_deltaX * deltaFactor);
        /**
         * Scroll y delta
         * @type {number}
         */
        this.deltaY = Math.sign(_deltaY * deltaFactor);
        /**
         * Scroll columns delta
         * @type {number}
         */
        this.deltaColumns = this.deltaX * moveByRange;
        /**
         * Scroll rows delta
         * @type {number}
         */
        this.deltaRows = -this.deltaY * moveByRange;
    }
}
