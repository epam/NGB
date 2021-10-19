import InteractiveZoneEvent from './interactive-zone-event';
import eventTypes from './event-types';

/**
 * @class InteractiveZoneDragEvent
 * @extends InteractiveZoneEvent
 */
export default class InteractiveZoneDragEvent extends InteractiveZoneEvent {
    /**
     *
     * @param {MouseEvent} event
     * @param {HeatmapViewport} viewport
     * @param {InteractiveZoneEvent} mouseDownEvent
     * @param {Object} [options] custom options
     */
    constructor(event, viewport, mouseDownEvent, options = {}) {
        super(event, viewport, options);
        this.name = eventTypes.drag;
        /**
         * Columns delta
         * @type {number}
         */
        this.columnsDelta = 0;
        /**
         * Rows delta
         * @type {number}
         */
        this.rowsDelta = 0;
        /**
         * Viewport columns center on drag start
         * @type {number}
         */
        this.dragStartViewportColumn = 0;
        /**
         * Viewport rows center on drag start
         * @type {number}
         */
        this.dragStartViewportRow = 0;
        /**
         * X coordinates delta
         * @type {number}
         */
        this.xDelta = 0;
        /**
         * Y coordinates delta
         * @type {number}
         */
        this.yDelta = 0;
        /**
         * X coordinate of drag start
         * @type {number}
         */
        this.dragStartX = 0;
        /**
         * Y coordinate of drag start
         * @type {number}
         */
        this.dragStartY = 0;
        /**
         * Column coordinate of drag start
         * @type {number}
         */
        this.dragStartColumn = 0;
        /**
         * Row coordinate of drag start
         * @type {number}
         */
        this.dragStartRow = 0;
        if (mouseDownEvent) {
            this.dragStartX = mouseDownEvent.x;
            this.dragStartY = mouseDownEvent.y;
            this.dragStartColumn = mouseDownEvent.column;
            this.dragStartRow = mouseDownEvent.row;
            this.xDelta = mouseDownEvent.clientX - this.clientX;
            this.yDelta = mouseDownEvent.clientY - this.clientY;
            this.dragStartViewportColumn = mouseDownEvent.viewportColumn;
            this.dragStartViewportRow = mouseDownEvent.viewportRow;
            this.columnsDelta = viewport.scale.getScaleDimension(this.xDelta);
            this.rowsDelta = viewport.scale.getScaleDimension(this.yDelta);
        }
    }
}
