import eventTypes from './event-types';

export default class InteractiveZoneEvent {
    /**
     *
     * @param {MouseEvent} event
     * @param {HeatmapViewport} viewport
     * @param {Object} [options]
     */
    constructor(event, viewport, options = {}) {
        /**
         * Event name
         * @type {string}
         */
        this.name = eventTypes.move;
        /**
         *
         * @type {MouseEvent}
         */
        this.nativeEvent = event;
        const point = {x: event.offsetX, y: event.offsetY};
        const {x, y} = viewport.getRelativeCanvasPoint(point);
        const {column, row} = viewport.getViewportPoint(point);
        /**
         * Mouse x position (relative to heatmap viewport)
         * @type {number}
         */
        this.x = x;
        /**
         * Mouse y position (relative to heatmap viewport)
         * @type {number}
         */
        this.y = y;
        /**
         * Mouse x position (relative to document)
         * @type {number}
         */
        this.clientX = event.clientX;
        /**
         * Mouse y position (relative to document)
         * @type {number}
         */
        this.clientY = event.clientY;
        /**
         * Mouse x position (relative to canvas)
         * @type {number}
         */
        this.globalX = point.x;
        /**
         * Mouse y position (relative to canvas)
         * @type {number}
         */
        this.globalY = point.y;
        /**
         * Shift key pressed
         * @type {boolean}
         */
        this.shift = event.shiftKey;
        /**
         * Mouse column position
         * @type {number}
         */
        this.column = column;
        /**
         * Mouse row position
         * @type {number}
         */
        this.row = row;
        /**
         * Viewport columns center
         * @type {number}
         */
        this.viewportColumn = viewport.columns.center;
        /**
         * Viewport rows center
         * @type {number}
         */
        this.viewportRow = viewport.rows.center;
        /**
         * Mouse fits viewport columns range
         * @type {function: boolean}
         */
        this.fitsColumns = () => viewport.columns.start <= this.column && this.column <= viewport.columns.end;
        /**
         * Mouse fits viewport rows range
         * @type {function: boolean}
         */
        this.fitsRows = () => viewport.rows.start <= this.row && this.row <= viewport.rows.end;
        /**
         * Mouse fits viewport range
         * @type {function: boolean}
         */
        this.fits = () => this.fitsColumns() && this.fitsRows();
        this.fitsViewport = () => this.x >= 0 &&
            this.y >= 0 &&
            this.x <= viewport.deviceWidth &&
            this.y <= viewport.deviceHeight;
        /**
         * Custom event options
         * @type {Object}
         */
        this.options = options;
        this._defaultPrevented = false;
        this._immediatePropagationStopped = false;
    }

    /**
     * Default behaviour is prevented
     * @return {boolean}
     */
    get defaultIsPrevented() {
        return this._defaultPrevented;
    }

    /**
     * Immediate propagation stopped
     * @return {boolean}
     */
    get immediatePropagationStopped() {
        return this._immediatePropagationStopped;
    }

    /**
     * Prevents default behaviour
     */
    preventDefault() {
        this._defaultPrevented = true;
    }

    /**
     * Stops immediate propagation
     */
    stopImmediatePropagation() {
        this._immediatePropagationStopped = true;
    }
}
