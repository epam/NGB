import * as PIXI from 'pixi.js-legacy';
import {getSessionFlags, updateSessionFlags} from '../utilities/viewport-session-flags';
import CollapsedTree from './collapsed-tree';
import HeatmapEventDispatcher from '../../../utilities/heatmap-event-dispatcher';
import cancellablePromise from '../utilities/cancellable-promise';
import events from '../../../utilities/events';
import makeInitializable from '../../../utilities/make-initializable';

const UPDATE_FRAME_ITERATIONS = 1000;
const pointInsideRange = (point, rangeStart, rangeEnd) => rangeStart <= point && point <= rangeEnd;

/**
 * @typedef {Object} HeatmapGraphicsOptions
 * @property {number} column
 * @property {number} row
 * @property {number} width
 * @property {number} height
 * @property {HeatmapViewport} viewport
 * @property {ColorScheme} colorScheme
 * @property {boolean} [renderWhileBuilding=true]
 * @property {boolean} [CANVAS_RENDERER=true]
 */

class HeatmapGraphicsBase extends HeatmapEventDispatcher {
    /**
     *
     * @param {HeatmapGraphicsOptions} options
     */
    constructor(options = {}) {
        super();
        makeInitializable(this);
        const {
            column,
            row,
            width,
            height,
            viewport,
            colorScheme,
            renderWhileBuilding = true
        } = options;
        this.clearSession();
        this.renderWhileBuilding = renderWhileBuilding;
        this._dirty = false;
        this._reset = false;
        this._changed = true;
        this.modifiedData = [];
        this.row = row;
        this.column = column;
        this.width = width;
        this.height = height;
        /**
         *
         * @type {CollapsedTree}
         */
        this.data = this.createTree();
        this.viewport = viewport;
        this.container = new PIXI.Container();
        /**
         *
         * @type {ColorScheme}
         */
        this.applyColorScheme(colorScheme);
        this.initialized = false;
        this.initialize()
            .then(this.requestRebuildAnimationFrame.bind(this));
    }

    createTree() {
        return new CollapsedTree(0, 0, this.width, this.height);
    }

    get partialRebuildRequired() {
        return this.modifiedData.length > 0;
    }

    get partialRebuildInProgress() {
        return this._partialRebuildInProgress;
    }

    get fullRebuildRequired() {
        return this._dirty;
    }

    get fullRebuildInProgress() {
        return this._fullRebuildInProgress;
    }

    get updating() {
        return this.partialRebuildInProgress || this.fullRebuildInProgress;
    }

    initializePixiObjects() {
        return Promise.resolve();
    }

    initialize() {
        return new Promise((resolve) => {
            this.clearSession();
            const cancelLayoutBuild = typeof this.cancelLayoutBuild === 'function'
                ? this.cancelLayoutBuild()
                : Promise.resolve();
            cancelLayoutBuild
                .then(() => this.initializePixiObjects())
                .then(() => {
                    this.requestRebuild();
                    this.initialized = true;
                    resolve();
                });
        });
    }

    onUpdating(callback) {
        this.addEventListener(events.updating, callback);
    }

    onRequestRender(callback) {
        this.addEventListener(events.render.request, callback);
    }

    requestRender() {
        this.emit(events.render.request);
    }

    destroy() {
        this.cancelRebuildAnimationFrame();
        this.colorScheme = undefined;
        this.viewport = undefined;
        this.data = undefined;
        super.destroy();
    }

    /**
     * Applies color scheme
     * @param {ColorScheme} colorScheme
     * @returns {boolean}
     */
    applyColorScheme(colorScheme) {
        /**
         * @type {ColorScheme}
         */
        this.colorScheme = colorScheme;
        this._dirty = this.data.hasValues();
        this.modifiedData = [];
        this._changed = true;
        this.requestRebuild();
        return true;
    }

    requestRebuild() {
        if (this._dirty && this._fullRebuildInProgress && this.modifiedData.length === 0) {
            return;
        }
        this.rebuildRequested = true;
    }

    requestRebuildAnimationFrame() {
        if (!this.destroyed) {
            this.checkRebuildFrame = requestAnimationFrame(this.rebuildAnimationFrame.bind(this));
        }
    }

    cancelRebuildAnimationFrame() {
        if (typeof this.cancelLayoutBuild === 'function') {
            this.cancelLayoutBuild();
        }
        cancelAnimationFrame(this.checkRebuildFrame);
    }

    rebuildAnimationFrame() {
        if (this.rebuildRequested && !this.destroyed) {
            this.rebuildRequested = false;
            const doFullRebuild = this.fullRebuildRequired && !this.fullRebuildInProgress;
            const doPartialRebuild = this.partialRebuildRequired && !this.partialRebuildInProgress;
            if (doFullRebuild || doPartialRebuild) {
                this.cancelLayoutBuild = cancellablePromise(
                    this.rebuild.bind(this),
                    this.cancelLayoutBuild
                );
            }
        }
        this.requestRebuildAnimationFrame();
    }

    /**
     *
     * @param {function: boolean} isCancelled
     */
    rebuild(isCancelled) {
        if (isCancelled()) {
            return Promise.resolve(false);
        }
        const partialRebuildPromise = () => new Promise((resolve) => {
            this.doPartialRebuild(isCancelled, resolve);
        });
        const fullRebuildPromise = () => new Promise((resolve) => {
            this.doFullRebuild(isCancelled, resolve);
        });
        return new Promise((resolve) => {
            partialRebuildPromise()
                .then(() => fullRebuildPromise())
                .then(resolve);
        });
    }

    getFullRebuildOptions(callback) {
        const colorScheme = this.colorScheme;
        return {
            colorScheme,
            reset: this._reset,
            // eslint-disable-next-line no-unused-vars
            callback: (cancelled = false) => {
                this._fullRebuildInProgress = false;
                this._changed = true;
                this.emit(events.updating);
                this.requestRender();
                callback();
            },
            omit: false
        };
    }

    // eslint-disable-next-line no-unused-vars
    appendGraphics(item, options) {
    }

    doFullRebuild(isCancelled, callback) {
        if (
            isCancelled() ||
            !this._dirty ||
            !this.colorScheme ||
            !this.colorScheme.initialized
        ) {
            callback();
            return;
        }
        this._fullRebuildInProgress = true;
        this.emit(events.updating);
        const generator = this.data.values();
        const options = this.getFullRebuildOptions(callback);
        if (options.omit) {
            this._dirty = false;
            this._reset = false;
            options.callback(false);
            return;
        }
        const frame = () => {
            const iterations = UPDATE_FRAME_ITERATIONS;
            let i = 0;
            let data;
            let done = false;
            const next = () => {
                const nextIteration = generator.next();
                done = nextIteration.done;
                data = nextIteration.value;
            };
            do {
                if (isCancelled()) {
                    break;
                }
                next();
                if (data && !done) {
                    this.appendGraphics(data, options);
                }
                i += 1;
            } while (i < iterations && !done);
            if (this.renderWhileBuilding) {
                this._changed = true;
                this.requestRender();
            }
            if (isCancelled()) {
                options.callback(true);
                return;
            }
            if (!done) {
                if (this.renderWhileBuilding) {
                    this.requestRender();
                }
                setTimeout(frame, 0);
            } else {
                this._dirty = false;
                this._reset = false;
                options.callback(false);
            }
        };
        frame();
    }

    getPartialRebuildOptions(callback) {
        const colorScheme = this.colorScheme;
        return {
            colorScheme,
            // eslint-disable-next-line no-unused-vars
            callback: (cancelled = false) => {
                this._partialRebuildInProgress = false;
                this._changed = true;
                this.emit(events.updating);
                callback();
            }
        };
    }

    doPartialRebuild(isCancelled, callback) {
        if (
            isCancelled() ||
            !this.colorScheme ||
            !this.colorScheme.initialized ||
            this._dirty
        ) {
            callback();
            return;
        }
        const options = this.getPartialRebuildOptions(callback);
        const frame = () => {
            this._partialRebuildInProgress = true;
            this.emit(events.updating);
            let i = 0;
            let item = {};
            while (i < UPDATE_FRAME_ITERATIONS && item) {
                item = this.modifiedData.pop();
                if (!item) {
                    break;
                }
                this.appendGraphics(item, options);
                if (isCancelled()) {
                    break;
                }
                i += 1;
            }
            if (this.renderWhileBuilding) {
                this._changed = true;
                this.requestRender();
            }
            if (isCancelled()) {
                options.callback(true);
                return;
            }
            if (this.modifiedData.length > 0) {
                setTimeout(frame, 0);
            } else {
                options.callback(false);
            }
        };
        frame();
    }

    batchInsertStart() {
        this.batchData = this.createTree();
    }

    /**
     * Appends data item to graphics
     * @param {HeatmapDataItem} item
     * @param {CollapsedTree} data
     * @returns {HeatmapDataItemWithSize|undefined}
     */
    insertDataItem(item, data) {
        if (!item || !data) {
            return undefined;
        }
        const {
            column,
            row,
            value
        } = item;
        const columnCorrected = column - this.column;
        const rowCorrected = row - this.row;
        return data.append({
            column: columnCorrected,
            row: rowCorrected,
            value
        });
    }

    /**
     * Appends data item to graphics
     * @param {HeatmapDataItem} item
     * @param {boolean} [batch=true]
     */
    appendDataItem(item, batch = true) {
        if (batch && !this.batchData) {
            this.batchData = this.createTree();
        }
        const data = batch ? this.batchData : this.data;
        const element = this.insertDataItem(item, data);
        if (!batch && element) {
            this.modifiedData.push(element);
            this.requestRebuild();
        }
    }

    batchUpdateDone(reset = false) {
        if (this.batchData) {
            this.data = undefined;
            this.data = this.batchData;
            this.batchData = undefined;
        }
        this._dirty = true;
        this._reset = reset;
        this.requestRebuild();
    }

    /**
     * Tests if data item belongs to current graphics (i.e. is within the graphics bounds)
     * @param {HeatmapDataItem} item
     * @returns {boolean}
     */
    testDataItem(item) {
        if (!item) {
            return false;
        }
        return item.column >= this.column && item.column < this.column + this.width &&
            item.row >= this.row && item.row < this.row + this.height;
    }

    clearSession() {
        this.session = {};
    }

    isVisible() {
        const viewportBounds = {
            x1: this.viewport.columns.getScalePosition(0),
            x2: this.viewport.columns.getScalePosition(this.viewport.deviceWidth),
            y1: this.viewport.rows.getScalePosition(0),
            y2: this.viewport.rows.getScalePosition(this.viewport.deviceHeight)
        };
        const bounds = {
            x1: this.column,
            x2: this.column + this.width,
            y1: this.row,
            y2: this.row + this.height
        };
        const columnsFit = pointInsideRange(bounds.x1, viewportBounds.x1, viewportBounds.x2) ||
            pointInsideRange(bounds.x2, viewportBounds.x1, viewportBounds.x2) ||
            (viewportBounds.x1 >= bounds.x1 && viewportBounds.x2 <= bounds.x2);
        const rowsFit = pointInsideRange(bounds.y1, viewportBounds.y1, viewportBounds.y2) ||
            pointInsideRange(bounds.y2, viewportBounds.y1, viewportBounds.y2) ||
            (viewportBounds.y1 >= bounds.y1 && viewportBounds.y2 <= bounds.y2);
        return columnsFit && rowsFit;
    }

    getSessionFlags() {
        const visible = this.isVisible();
        const visibilityChanged = this.container.visible !== visible;
        const viewportChanged = getSessionFlags(this.session, this.viewport);
        return {
            graphicsChanged: this._changed,
            viewportChanged,
            visibilityChanged
        };
    }

    updateSessionFlags() {
        this._changed = false;
        this.session = updateSessionFlags(this.viewport);
    }

    render() {
        const {
            visibilityChanged,
            viewportChanged,
            graphicsChanged
        } = this.getSessionFlags();
        this.container.visible = this.isVisible();
        if (viewportChanged) {
            this.container.scale.set(this.viewport.scale.tickSize);
            this.container.x = this.viewport.columns.getDevicePosition(this.column);
            this.container.y = this.viewport.rows.getDevicePosition(this.row);
        }
        this.updateSessionFlags();
        return viewportChanged || visibilityChanged || graphicsChanged;
    }
}

export default HeatmapGraphicsBase;
