import * as PIXI from 'pixi.js-legacy';
import {ColumnsRenderer, RowsRenderer} from './axis-renderer';
import InteractiveZone from '../../interactions/interactive-zone';
import config from './config';
import events from '../../utilities/events';
import makeInitializable from '../../utilities/make-initializable';

class HeatmapViewportRenderer extends InteractiveZone {
    /**
     *
     * @param {HeatmapViewOptions} options
     * @param {HeatmapViewport} viewport
     * @param {HeatmapInteractions} interactions
     * @param {LabelsManager} labelsManager
     */
    constructor(options, viewport, interactions, labelsManager) {
        super({priority: InteractiveZone.Priorities.viewport});
        makeInitializable(this);
        /**
         * Heatmap viewport
         * @type {HeatmapViewport}
         */
        this.viewport = viewport;
        this.labelsManager = labelsManager;
        this.container = new PIXI.Container();
        /**
         *
         * @type {HeatmapViewOptions}
         */
        this.options = options;
        this.annotationsDisplayChangedCallback = () => {
            if (this.options && this.options.data && this.options.data.metadata) {
                this.initialize(this.options.data.metadata);
            }
        };
        if (this.options) {
            this.options.onAnnotationsModeChanged(this.annotationsDisplayChangedCallback);
        }
        this.columnAxis = new ColumnsRenderer(options, interactions, viewport, labelsManager);
        this.rowAxis = new RowsRenderer(options, interactions, viewport, labelsManager);
        this.columnAxis.onAxisItemClick((axis, payload) => this.emit(events.click, {column: payload}));
        this.rowAxis.onAxisItemClick((axis, payload) => this.emit(events.click, {row: payload}));
        this.columnAxis.onLayout(() => this.emit(events.layout));
        this.rowAxis.onLayout(() => this.emit(events.layout));
        this.interactions = interactions;
        this.interactions.registerInteractiveZone(this);

        this.selection = new PIXI.Graphics();
        this.container.addChild(this.columnAxis.container);
        this.container.addChild(this.rowAxis.container);
        this.container.addChild(this.selection);

        this.selectionInfo = undefined;

        this.initialized = false;
        const onInitialized = () => {
            this.initialized = this.columnAxis.initialized && this.rowAxis.initialized;
        };
        this.columnAxis.onInitialized(onInitialized);
        this.rowAxis.onInitialized(onInitialized);
    }

    destroy() {
        this.interactions.unregisterInteractiveZone(this);
        if (this.container) {
            this.container.destroy();
        }
        this.columnAxis.destroy();
        this.rowAxis.destroy();
        this.labelsManager = undefined;
        if (this.options) {
            this.options.removeEventListeners(this.annotationsDisplayChangedCallback);
        }
        this.options = undefined;
        super.destroy();
    }

    test(event) {
        return event && event.fitsViewport();
    }

    shouldDrag(event) {
        return event.fitsViewport() && event.shift;
    }

    onDragStart(event) {
        event.stopImmediatePropagation();
        super.onDragStart(event);
    }

    onDrag(event) {
        super.onDrag(event);
        this.selectionInfo = {
            start: {
                column: Math.floor(this.viewport.columns.getCorrectedPositionWithinAxis(event.dragStartColumn)),
                row: Math.floor(this.viewport.rows.getCorrectedPositionWithinAxis(event.dragStartRow))
            },
            end: {
                column: Math.floor(this.viewport.columns.getCorrectedPositionWithinAxis(event.column)),
                row: Math.floor(this.viewport.rows.getCorrectedPositionWithinAxis(event.row))
            }
        };
        this._selectionChanged = true;
        this.requestRender();
        super.onDrag(event);
    }

    onDragEnd(event) {
        event.stopImmediatePropagation();
        this.selectionInfo = undefined;
        this._selectionChanged = true;
        this.requestRender();
        const [c1, c2] = [
            Math.floor(this.viewport.columns.getCorrectedPositionWithinAxis(event.dragStartColumn)),
            Math.floor(this.viewport.columns.getCorrectedPositionWithinAxis(event.column))
        ].sort((a, b) => a - b);
        const [r1, r2] = [
            Math.floor(this.viewport.rows.getCorrectedPositionWithinAxis(event.dragStartRow)),
            Math.floor(this.viewport.rows.getCorrectedPositionWithinAxis(event.row))
        ].sort((a, b) => a - b);
        this.viewport.zoomToViewport(c1, c2 + 1, r1, r2 + 1);
        super.onDragEnd(event);
    }

    onAxisClick(callback) {
        this.addEventListener(events.click, callback);
    }

    onLayout(callback) {
        this.addEventListener(events.layout, callback);
    }

    /**
     * Initializes axis labels
     * @param {Object} [metadata]
     * @param {Array<string>} [metadata.columns = []]
     * @param {Array<string>} [metadata.rows = []]
     */
    initialize(metadata = {}) {
        const {
            columns = [],
            rows = []
        } = metadata;
        const showAnnotations = !this.options || this.options.annotations;
        this.columnAxis.initialize(columns, showAnnotations);
        this.rowAxis.initialize(rows, showAnnotations);
    }

    /**
     * @typedef {Object} LayoutProperty
     * @property {number} column
     * @property {number} row
     */

    /**
     * @typedef {Object} LayoutInfo
     * @property {LayoutProperty} offset
     * @property {LayoutProperty} size
     * @property {LayoutProperty} available
     */

    /**
     *
     * @param {{width: number, height: number}|undefined} size
     * @param {{top: number, right: number, bottom: number, left: number}} padding
     * @returns {LayoutInfo|undefined}
     */
    getLayoutInfo(size = {}, padding = {}) {
        const {
            width = 0,
            height = 0
        } = size || {};
        if (width && height) {
            const {
                top = 0,
                right = 0,
                bottom = 0,
                left = 0
            } = padding || {};
            const columnLayout = this.columnAxis.getLayoutInfo(
                height,
                {start: top, end: bottom}
            );
            const rowLayout = this.rowAxis.getLayoutInfo(
                width,
                {start: left, end: right}
            );
            if (columnLayout && rowLayout) {
                const {
                    endMargin: rEndMargin = bottom,
                    startMargin: rStartMargin = top,
                    available: rAvailable = height - rStartMargin - rEndMargin,
                    required: cRequired = Infinity
                } = columnLayout;
                const {
                    endMargin: cEndMargin = right,
                    startMargin: cStartMargin = left,
                    available: cAvailable = width - cStartMargin - cEndMargin,
                    required: rRequired = Infinity
                } = rowLayout;
                const cSize = Math.min(cAvailable, cRequired);
                const rSize = Math.min(rAvailable, rRequired);
                const cOffset = cStartMargin + (cAvailable - cSize) / 2.0;
                const rOffset = rStartMargin + (rAvailable - rSize) / 2.0;
                return {
                    offset: {
                        column: cOffset,
                        row: rOffset
                    },
                    size: {
                        column: cSize,
                        row: rSize
                    },
                    available: {
                        column: cAvailable,
                        row: rAvailable
                    }
                };
            }
        }
        return undefined;
    }

    renderSelection() {
        const selectionChanged = this._selectionChanged;
        if (selectionChanged) {
            this.selection.clear();
            if (this.selectionInfo) {
                const {
                    start,
                    end,
                } = this.selectionInfo;
                const {
                    column: cStart = this.viewport.columns.start,
                    row: rStart = this.viewport.rows.start
                } = start || {};
                const {
                    column: cEnd = this.viewport.columns.start,
                    row: rEnd = this.viewport.rows.start
                } = end || {};
                const [x1, x2] = [
                    this.viewport.columns.getDevicePosition(cStart),
                    this.viewport.columns.getDevicePosition(cEnd + 1)
                ].sort((a, b) => a - b);
                const [y1, y2] = [
                    this.viewport.rows.getDevicePosition(rStart),
                    this.viewport.rows.getDevicePosition(rEnd + 1)
                ].sort((a, b) => a - b);
                this.selection
                    .beginFill(config.selection.fill, config.selection.alpha)
                    .lineStyle(1, config.selection.fill, 1)
                    .drawRect(
                        x1,
                        y1,
                        x2 - x1,
                        y2 - y1
                    )
                    .endFill();
            }
        }
        this._selectionChanged = false;
        return selectionChanged;
    }

    render() {
        let somethingChanged = this.columnAxis.render();
        somethingChanged = this.rowAxis.render() || somethingChanged;
        somethingChanged = this.renderSelection() || somethingChanged;
        return somethingChanged;
    }
}

export default HeatmapViewportRenderer;
