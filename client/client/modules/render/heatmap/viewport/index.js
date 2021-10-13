import HeatmapAxis from './heatmap-axis';
import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import HeatmapScale from './heatmap-scale';
import events from '../utilities/events';

const VIEWPORT_CHANGED = Symbol('viewport changed');
const DEFAULT_ZOOM_RATIO = 2;
const MINIMUM_AXIS_SIZE = 75;
const BEST_FIT_THRESHOLD = 0.5;

/**
 * @typedef {Object} ViewportPoint
 * @property {number} column
 * @property {number} row
 */

/**
 * @typedef {Object} CanvasPoint
 * @property {number} x
 * @property {number} y
 */

const ZeroCanvasPoint = {x: 0, y: 0};
const ZeroViewportPoint = {column: 0, row: 0};

/**
 * @typedef {Object} HeatmapViewportOptions
 * @property {number} [columns = 0] - number of columns
 * @property {number} [rows = 0] - number of rows
 * @property {number} [deviceWidth = 0] - device width (pixels)
 * @property {number} [deviceHeight = 0] - device height (pixels)
 * @property {boolean} [fit=false] - fit viewport
 */

export default class HeatmapViewport extends HeatmapEventDispatcher {
    constructor(options = {}) {
        super();
        this._offsetX = 0;
        this._offsetY = 0;
        /**
         * Scaler
         * @type {HeatmapScale}
         */
        this.scale = new HeatmapScale();
        /**
         * Columns axis
         * @type {HeatmapAxis}
         */
        this.columns = new HeatmapAxis({scale: this.scale});
        /**
         * Rows axis
         * @type {HeatmapAxis}
         */
        this.rows = new HeatmapAxis({scale: this.scale});
        this[VIEWPORT_CHANGED] = false;
        const viewportChanged = () => this[VIEWPORT_CHANGED] = true;
        this.scale.onScale(viewportChanged);
        this.columns.onMove(viewportChanged);
        this.rows.onMove(viewportChanged);
        this.initialize(options);
        this.checkViewportChanged();
    }

    get deviceWidth() {
        return this.columns.deviceSize;
    }

    set deviceWidth(deviceWidth) {
        this.columns.deviceSize = deviceWidth;
    }

    get deviceHeight() {
        return this.rows.deviceSize;
    }

    set deviceHeight(deviceHeight) {
        this.rows.deviceSize = deviceHeight;
    }

    get deviceAvailableWidth() {
        return this._deviceAvailableWidth || this.deviceWidth;
    }

    set deviceAvailableWidth(deviceAvailableWidth) {
        this._deviceAvailableWidth = deviceAvailableWidth;
        this.checkMinimumTickSize();
    }

    get deviceAvailableHeight() {
        return this._deviceAvailableHeight || this.deviceHeight;
    }

    set deviceAvailableHeight(deviceAvailableHeight) {
        this._deviceAvailableHeight = deviceAvailableHeight;
        this.checkMinimumTickSize();
    }

    get offsetX() {
        return this._offsetX;
    }

    set offsetX(x) {
        if (this._offsetX !== x) {
            this._offsetX = x;
            setTimeout(this.emit.bind(this, events.changed), 0);
        }
    }

    get offsetY() {
        return this._offsetY;
    }

    set offsetY(y) {
        if (this._offsetY !== y) {
            this._offsetY = y;
            setTimeout(this.emit.bind(this, events.changed), 0);
        }
    }

    get invalid() {
        return !this.scale ||
            this.scale.invalid ||
            !this.columns ||
            !this.rows ||
            this.columns.invalid ||
            this.rows.invalid;
    }

    get description() {
        return `columns: ${this.columns.description}; rows: ${this.rows.description}; scale: ${this.scale.tickSize}`;
    }

    destroy() {
        super.destroy();
        if (this.viewportChangedAnimationFrame) {
            cancelAnimationFrame(this.viewportChangedAnimationFrame);
        }
    }

    /**
     * Gets canvas point relative to viewport
     * @param {CanvasPoint} point
     * @returns {CanvasPoint}
     */
    getRelativeCanvasPoint(point) {
        return {
            x: point.x - this.offsetX,
            y: point.y - this.offsetY
        };
    }

    /**
     * Gets global canvas point from viewport canvas point
     * @param {CanvasPoint} point
     * @returns {CanvasPoint}
     */
    getGlobalCanvasPoint(point) {
        return {
            x: point.x + this.offsetX,
            y: point.y + this.offsetY
        };
    }

    /**
     * Translates canvas point to viewport point
     * @param {CanvasPoint} canvasPoint
     * @returns {ViewportPoint}
     */
    getViewportPoint(canvasPoint) {
        if (this.invalid) {
            return ZeroViewportPoint;
        }
        const relative = this.getRelativeCanvasPoint(canvasPoint);
        return {
            column: this.columns.getScalePosition(relative.x),
            row: this.rows.getScalePosition(relative.y)
        };
    }

    /**
     * Translates viewport point to canvas point
     * @param {ViewportPoint} viewportPoint
     * @returns {CanvasPoint}
     */
    getCanvasPoint(viewportPoint) {
        if (this.invalid) {
            return ZeroCanvasPoint;
        }
        return {
            x: this.offsetX + this.columns.getDevicePosition(viewportPoint.column),
            y: this.offsetY + this.rows.getDevicePosition(viewportPoint.row)
        };
    }

    /**
     * Initialize viewport axis (columns & rows)
     * @param options {HeatmapViewportOptions}
     */
    initialize(options = {}) {
        const {
            columns = this.columns.size,
            rows = this.rows.size,
            deviceWidth = this.deviceWidth,
            deviceHeight = this.deviceHeight,
            fit = false
        } = options;
        const changed = columns !== this.columns.size ||
            rows !== this.rows.size ||
            deviceWidth !== this.deviceWidth ||
            deviceHeight !== this.deviceHeight;
        this.columns.initialize({size: columns, deviceSize: deviceWidth});
        this.rows.initialize({size: rows, deviceSize: deviceHeight});
        if (changed || fit) {
            this.fit(false);
        }
    }

    checkViewportChanged() {
        if (this[VIEWPORT_CHANGED]) {
            this[VIEWPORT_CHANGED] = false;
            setTimeout(() => this.emit(events.changed), 0);
        }
        this.viewportChangedAnimationFrame = requestAnimationFrame(this.checkViewportChanged.bind(this));
    }

    onViewportChanged(callback) {
        this.addEventListener(
            events.changed,
            callback
        );
        return this;
    }

    /**
     * Makes an axis to fit device
     * @param {HeatmapAxis} axis
     * @param {boolean} animated
     * @param {number} [fitToSize = 0]
     * @return {boolean}
     */
    fitAxis(axis, animated = true, fitToSize = 0) {
        if (this.invalid || axis.invalid) {
            return false;
        }
        const ratio = (fitToSize || axis.deviceSize) / axis.totalDeviceSize;
        return this.zoom({ratio, anchor: {column: 0, row: 0}}, animated);
    }

    /**
     * Returns array of objects {axis, available, tickToFit} of axis, it's available space in pixels and
     * tick size to make axis fully visible; array is sorted (inc.) by tickToFit value
     * @returns {{available: number, axis: HeatmapAxis, tickToFit: number}[]}
     */
    getFitCoverConfiguration() {
        if (this.invalid) {
            return [];
        }
        return [
            {axis: this.columns, available: this.deviceAvailableWidth},
            {axis: this.rows, available: this.deviceAvailableHeight}
        ]
            .map(o => ({...o, available: o.available || o.axis.deviceSize}))
            .map(o => ({...o, tickToFit: o.available / o.axis.size}))
            .filter(o => !Number.isNaN(Number(o.tickToFit)) && Number.isFinite(Number(o.tickToFit)))
            .sort((a, b) => a.available - b.available);
    }

    fit(animated = true) {
        if (this.invalid) {
            return false;
        }
        const [axis] = this.getFitCoverConfiguration();
        if (axis) {
            return this.fitAxis(axis.axis, animated, axis.available);
        }
        return false;
    }

    cover(animated = true) {
        if (this.invalid) {
            return false;
        }
        const axis = this.getFitCoverConfiguration().pop();
        if (axis) {
            return this.fitAxis(axis.axis, animated, axis.available);
        }
        return false;
    }

    checkMinimumTickSize() {
        if (this.columns.invalid || this.rows.invalid || this.scale.invalid) {
            return;
        }
        const columnsMinimumTickSize = this.deviceAvailableWidth / this.columns.size;
        const rowsMinimumTickSize = this.deviceAvailableHeight / this.rows.size;
        const minimumTickSize = Math.min(
            columnsMinimumTickSize,
            rowsMinimumTickSize
        );
        const columnsSize = Math.max(MINIMUM_AXIS_SIZE, minimumTickSize * this.columns.size);
        const rowsSize = Math.max(MINIMUM_AXIS_SIZE, minimumTickSize * this.rows.size);
        this.scale.minimumTickSize = Math.max(
            columnsSize / this.columns.size,
            rowsSize / this.rows.size
        );
    }

    best(animated = true) {
        if (this.invalid) {
            return false;
        }
        const max = Math.max(this.columns.size, this.rows.size);
        const min = Math.min(this.columns.size, this.rows.size);
        const ratio = min / max;
        if (ratio < BEST_FIT_THRESHOLD) {
            return this.cover(animated);
        }
        return this.fit(animated);
    }

    /**
     * Zooms viewport
     * @param {Object} [options]
     * @param {number} [options.ratio = 1]
     * @param {Object} [options.anchor]
     * @param {number} [options.anchor.row]
     * @param {number} [options.anchor.column]
     * @param {boolean} [animate = true]
     * @return {boolean|*}
     */
    zoom(options = {}, animate= true) {
        if (this.invalid) {
            return false;
        }
        const {
            anchor = {},
            ratio = 1
        } = options;
        if (ratio === 1) {
            // no op
            return false;
        }
        const {column, row} = anchor;
        const newTickSize = this.scale.tickSize * Math.max(0, ratio);
        const futureScale = this.scale.getFutureScale(newTickSize);
        return [
            this.columns.preservePositionOnScale({anchor: column, futureScale}, animate),
            this.rows.preservePositionOnScale({anchor: row, futureScale}, animate),
            this.scale.setTickSize(newTickSize, animate)
        ].filter(Boolean).length > 0;
    }

    /**
     * Zooms viewport in
     * @param {{[row]: number, [column]: number}} [anchor]
     * @return {boolean}
     */
    zoomIn(anchor) {
        if (this.invalid) {
            return false;
        }
        return this.zoom({ratio: DEFAULT_ZOOM_RATIO, anchor});
    }

    /**
     * Zooms viewport out
     * @param {{[row]: number, [column]: number}} [anchor]
     * @return {boolean}
     */
    zoomOut(anchor) {
        if (this.invalid) {
            return false;
        }
        return this.zoom({ratio: 1.0 / DEFAULT_ZOOM_RATIO, anchor});
    }

    zoomToViewport(columnStart, columnEnd, rowStart, rowEnd, animate = true) {
        const columnCenter = (columnStart + columnEnd) / 2.0;
        const rowCenter = (rowStart + rowEnd) / 2.0;
        const columnsTickSize = this.deviceAvailableWidth / (columnEnd - columnStart);
        const rowsTickSize = this.deviceAvailableHeight / (rowEnd - rowStart);
        const newTickSize = Math.min(rowsTickSize, columnsTickSize);
        return [
            this.columns.move({center: columnCenter}, animate),
            this.rows.move({center: rowCenter}, animate),
            this.scale.setTickSize(newTickSize, animate)
        ].filter(Boolean).length > 0;
    }
}
