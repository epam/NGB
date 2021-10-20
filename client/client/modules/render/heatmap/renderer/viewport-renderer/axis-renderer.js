import * as PIXI from 'pixi.js-legacy';
import AxisLabel from './utilities/axis-label';
import AxisVectors from '../../utilities/axis-vectors';
import {EventTypes} from '../../interactions/events';
import HeatmapScaleScroller from './scroller-renderer';
import InteractiveZone from '../../interactions/interactive-zone';
import cancellablePromise from '../data-renderer/utilities/cancellable-promise';
import config from './config';
import events from '../../utilities/events';
import makeInitializable from '../../utilities/make-initializable';
import {movePoint} from '../../utilities/vector-utilities';

class HeatmapAxisRenderer extends InteractiveZone {
    /**
     *
     * @param {HeatmapViewOptions} options
     * @param {HeatmapInteractions} interactions
     * @param {HeatmapAxis} axis
     * @param {LabelsManager} labelsManager
     * @param {{[x]: number, [y]: number}} [direction]
     * @param {{[x]: number, [y]: number}} [normal]
     */
    constructor(
        options,
        interactions,
        axis,
        labelsManager,
        direction = {},
        normal = {}
    ) {
        super({
            priority: InteractiveZone.Priorities.scale
        });
        makeInitializable(this);
        /**
         *
         * @type {HeatmapViewOptions}
         */
        this.options = options;
        /**
         * Scroller
         * @type {HeatmapScaleScroller}
         */
        this.scroller = new HeatmapScaleScroller(axis, direction, normal);
        /**
         *
         * @type {HeatmapInteractions}
         */
        this.interactions = interactions;
        if (this.interactions) {
            this.interactions.registerInteractiveZone(this);
            this.interactions.registerInteractiveZone(this.scroller);
        }
        this.onDendrogramModeChangedCallback = this.onDendrogramModeChanged.bind(this);
        if (this.options) {
            this.options.onDendrogramModeChanged(this.onDendrogramModeChangedCallback);
        }
        this.axis = axis;
        this.labelsManager = labelsManager;
        this.container = new PIXI.Container();
        this.labelsContainer =new PIXI.Container();
        this.container.addChild(this.labelsContainer);
        this.container.addChild(this.scroller.container);
        const {x = 1, y = 0} = direction;
        this.direction = {x, y};
        this.normal = normal;
        this.directionRadians = Math.atan2(this.direction.y, this.direction.x);
        this.normalRadians = Math.atan2(this.normal.y, this.normal.x);
        /**
         *
         * @type {AxisLabel[]}
         */
        this.ticks = [];
        /**
         * Hovered tick
         * @type {undefined|AxisLabel}
         * @private
         */
        this._hoveredTick = undefined;
        this.clearRenderSession();
        this.initialize();
    }

    /**
     * Hovered tick
     * @returns {undefined|AxisLabel}
     */
    get hoveredTick() {
        return this._hoveredTick;
    }

    destroy() {
        if (typeof this.cancelInitialization === 'function') {
            this.cancelInitialization();
        }
        if (this.ticks) {
            this.ticks.forEach(tick => tick.destroy());
            this.ticks = [];
        }
        if (this.options) {
            this.options.removeEventListeners(this.onDendrogramModeChangedCallback);
        }
        if (this.interactions) {
            this.interactions.unregisterInteractiveZone(this);
            this.interactions.unregisterInteractiveZone(this.scroller);
        }
        if (this.scroller) {
            this.scroller.destroy();
        }
        if (this.container) {
            this.container.destroy();
        }
        this.scroller = undefined;
        this.interactions = undefined;
        this.options = undefined;
        this.labelsManager = undefined;
        super.destroy();
    }

    get axisSize() {
        if (
            Number.isNaN(Number(this._labelsMaxWidth)) ||
            !Number.isFinite(Number(this._labelsMaxWidth)) ||
            Number(this._labelsMaxWidth) <= 0
        ) {
            return config.maxAxisSize;
        }
        return Math.min(
            this.options && this.options.dendrogram ? Infinity : config.maxAxisSize,
            Number(this._labelsMaxWidth) + this.scroller.size
        );
    }

    get actualAxisSize() {
        if (this.labelFitsTick && this.direction.y === 0) {
            return this._labelsMaxHeight + this.scroller.size;
        }
        return this.axisSize;
    }

    get labelFitsTick () {
        return !Number.isNaN(Number(this._labelsMaxWidth)) &&
            this._labelsMaxWidth <= this.axis.scale.tickSize;
    }

    get labelExtraRotation () {
        if (
            !(this.options && this.options.dendrogram) &&
            !Number.isNaN(Number(this._labelsMaxWidth)) &&
            this._labelsMaxWidth + this.scroller.size > config.maxAxisSize
        ) {
            return Math.min(
                Math.PI / 4.0,
                Math.acos(config.maxAxisSize / (this._labelsMaxWidth + this.scroller.size))
            );
        }
        return 0;
    }

    onAxisItemClick(callback) {
        this.addEventListener(events.click, callback);
    }

    onLayout(callback) {
        this.addEventListener(events.layout, callback);
    }

    onDendrogramModeChanged() {
        this.emit(events.layout);
    }

    /**
     * Initializes axis labels
     * @param {HeatmapAnnotatedIndex[]} [labels = []]
     * @param {boolean} [showAnnotations=false]
     */
    initialize(labels = [], showAnnotations = true) {
        if (labels.length === 0) {
            return;
        }
        const options = {
            labelsManager: this.labelsManager,
            axis: this.axis,
            direction: this.direction,
            normal: this.normal,
            showAnnotations
        };
        const updateFn = (isCancelledFn) => {
            this.scroller.initialize();
            this.labelsContainer.removeChildren();
            this._hoveredTick = undefined;
            this.ticks.forEach(tick => tick.destroy());
            this.ticks = [];
            const partialUpdate = ticks => {
                if (!isCancelledFn()) {
                    ticks.forEach(tick => {
                        tick.visible = false;
                        tick.hovered = false;
                        tick.render();
                        this.ticks.push(tick);
                        this.labelsContainer.addChild(tick.container);
                    });
                    const currentLabelMaxWidth = this._labelsMaxWidth;
                    const currentLabelMaxHeight = this._labelsMaxHeight;
                    this.calculateAxisSize();
                    const reportLayoutChange = currentLabelMaxWidth !== this._labelsMaxWidth ||
                        currentLabelMaxHeight !== this._labelsMaxHeight;
                    this.updateTicksPositions();
                    this.updateTicksVisibility();
                    requestAnimationFrame(() => {
                        this.clearRenderSession();
                        this.initialized = true;
                        this.render();
                        this.requestRender();
                        if (reportLayoutChange) {
                            this.emit(events.layout);
                        }
                    });
                }
            };
            return new Promise((resolve) => {
                AxisLabel.initializeTicks(isCancelledFn, options, labels, partialUpdate)
                    .then(this.calculateAxisSize.bind(this))
                    .then(() => resolve())
                    .then(() => this.emit(events.layout));
            });
        };
        this.cancelInitialization = cancellablePromise(
            updateFn,
            this.cancelInitialization,
        );
    }

    calculateAxisSize () {
        this._labelsMaxWidth = Math.max(
            0,
            ...this.ticks.map(tick => tick.width)
        );
        this._labelsMaxHeight = Math.max(
            0,
            ...this.ticks.map(tick => tick.height)
        );
    }

    clearRenderSession() {
        this.session = {};
    }

    /**
     *
     * @param {InteractiveZoneEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    getHoveredValue(event) {
        return undefined;
    }

    /**
     *
     * @param {Point2D} point
     */
    pointOverAxis(point) {
        if (point) {
            const start = 0;
            const end = this.axisSize;
            const x = point.x * this.normal.x;
            const y = point.y * this.normal.y;
            return (start <= x && x <= end) && (start <= y && y <= end);
        }
        return false;
    }

    /**
     *
     * @param {InteractiveZone} event
     */
    getHoveredAxisItem(event) {
        const fitsAxisRange = o => this.axis && this.axis.start <= o && this.axis.end >= o;
        let value = this.getHoveredValue(event);
        if (!fitsAxisRange(value)) {
            value = undefined;
        }
        const pointOverAxis = this.pointOverAxis(event);
        let axisItem = undefined;
        if (pointOverAxis && this.labelsContainer) {
            const point = movePoint(event, {x: -this.labelsContainer.x, y: -this.labelsContainer.y});
            const visibleTicks = this.ticks.filter(tick => fitsAxisRange(tick.value));
            for (let t = 0; t < visibleTicks.length; t += 1) {
                if (visibleTicks[t].test(point)) {
                    value = visibleTicks[t].value;
                    axisItem = visibleTicks[t];
                    break;
                }
            }
        }
        return {value, axisItem, overAxis: pointOverAxis};
    }

    onHover(event) {
        const {value, axisItem: tooltip} = this.getHoveredAxisItem(event);
        const {
            value: hoveredValue
        } = this._hoveredTick || {};
        if (tooltip !== this.tooltipTick) {
            /**
             *
             * @type {AxisLabel}
             */
            this.tooltipTick = tooltip;
            if (!this.tooltipTick) {
                this.emit(events.tooltip.hide);
            } else {
                this.emit(events.tooltip.show,
                    {
                        event,
                        content: this.tooltipTick.getTooltip()
                    }
                );
            }
        }
        if (value !== hoveredValue) {
            const [tick] = this.ticks.filter(tick => tick.value === value);
            if (this._hoveredTick) {
                this._hoveredTick.hovered = false;
            }
            this._hoveredTick = tick;
            if (this._hoveredTick) {
                this._hoveredTick.hovered = true;
            }
            this.requestRender();
        }
    }

    onClick(event) {
        super.onClick(event);
        const {value, overAxis} = this.getHoveredAxisItem(event);
        if (overAxis && value !== undefined) {
            this.emit(events.click, value);
        }
    }

    /**
     *
     * @param {InteractiveZoneScrollEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    getScrollValue(event) {
        return 0;
    }

    onScroll(event) {
        super.onScroll(event);
        const moveBy = this.getScrollValue(event);
        if (this.axis.moveBy(moveBy, false)) {
            event.preventDefault();
        }
    }

    /**
     *
     * @param {InteractiveZoneDragEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    getDragValue(event) {
        return this.axis.center;
    }

    // eslint-disable-next-line no-unused-vars
    onDrag(event) {
        super.onDrag(event);
        this.axis.move(
            {
                center: this.getDragValue(event)
            },
            false
        );
    }

    /**
     * Returns layout dimensions (offsets and sizes) for axis:
     * * startMargin: space required for the axis to draw labels (respecting `margin.start`) (*in perpendicular direction*)
     * * available: available space *in perpendicular direction* based on passed `deviceSize`, `margin.end` and calculated `startMargin`
     * * endMargin: the same as passed `margin.end` (*in perpendicular direction*)
     * * required: required space for the axis to draw its graphics (*in axis direction*, does not depend on `deviceSize`, `margin`)
     * @param {number} deviceSize
     * @param {{start: number, end: number}} margin
     * @returns {{available: number, required: number, endMargin: number, startMargin: number}|undefined}
     */
    getLayoutInfo(deviceSize, margin = {}) {
        if (this.axis.invalid) {
            return undefined;
        }
        const {
            start: startMargin = 0,
            end: endMargin = 0
        } = margin;
        const available = deviceSize
            - this.axisSize
            - startMargin
            - endMargin;
        const required = this.axis.scale.getDeviceDimension(this.axis.size);
        return {
            startMargin: this.axisSize + startMargin,
            available,
            endMargin: endMargin,
            required
        };
    }

    /**
     * Gets viewport bounds for axis
     * @returns {{start: number, end: number}}
     */
    getViewportBounds() {
        if (this.axis && !this.axis.invalid) {
            return {
                start: this.axis.getDevicePosition(this.axis.start),
                end: this.axis.getDevicePosition(this.axis.end)
            };
        }
        return {start: Infinity, end: -Infinity};
    }

    updateTicksPositions() {
        return this.ticks.map(tick => tick.updatePosition({
            extraRotation: this.labelExtraRotation,
            fitsTick: this.labelFitsTick
        }))
            .filter(Boolean)
            .length > 0;
    }

    updateTicksVisibility() {
        const viewportBounds = this.getViewportBounds();
        const range = {};
        return this.ticks.map(tick => tick.updateVisibility(range, viewportBounds, this.hoveredTick))
            .filter(Boolean)
            .length > 0;
    }

    translate() {
        const shift = this.axis.getDevicePosition(0);
        const offset = this.scroller.size;
        this.labelsContainer.x = this.direction.x * shift + this.normal.x * offset;
        this.labelsContainer.y = this.direction.y * shift + this.normal.y * offset;
    }

    render() {
        if (!this.initialized) {
            return false;
        }
        const dendrogramChanged = !!this.options && this.session.dendrogram !== this.options.dendrogram;
        const scaleChanged = this.session.scale !== this.axis.scale.tickSize;
        const positionChanged = scaleChanged ||
            this.session.center !== this.axis.center ||
            this.session.deviceSize !== this.axis.deviceSize;
        const hoveredTickValue = this.hoveredTick ? this.hoveredTick.value : undefined;
        const hoverChanged = this.session.hovered !== hoveredTickValue;
        const visibilityChanged = (scaleChanged || hoverChanged) && !this.axis.isAnimating;
        let changed = false;
        if (scaleChanged || dendrogramChanged) {
            changed = this.updateTicksPositions() || changed;
        }
        if (positionChanged || dendrogramChanged || hoverChanged || visibilityChanged) {
            this.translate();
            changed = this.updateTicksVisibility() || changed;
        }
        changed = this.ticks
            .map(tick => tick.render())
            .filter(Boolean)
            .length > 0 || changed;
        this.session.scale = this.axis.scale.tickSize;
        this.session.center = this.axis.center;
        this.session.deviceSize = this.axis.deviceSize;
        this.session.hovered = hoveredTickValue;
        this.session.dendrogram = !!this.options && this.options.dendrogram;
        const scrollerChanged = this.scroller ? this.scroller.render() : false;
        return scaleChanged ||
            positionChanged ||
            hoverChanged ||
            visibilityChanged ||
            dendrogramChanged ||
            scrollerChanged ||
            changed;
    }
}

export class ColumnsRenderer extends HeatmapAxisRenderer {
    /**
     *
     * @param {HeatmapViewOptions} options
     * @param {HeatmapInteractions} interactions
     * @param {HeatmapViewport} viewport
     * @param {LabelsManager} labelsManager
     */
    constructor(options, interactions, viewport, labelsManager) {
        super(
            options,
            interactions,
            viewport.columns,
            labelsManager,
            AxisVectors.columns.direction,
            AxisVectors.columns.normal
        );
    }

    test(event) {
        return event && (event.name !== EventTypes.drag || event.fitsColumns());
    }

    getHoveredValue(event) {
        return event && event.column !== undefined
            ? Math.floor(event.column)
            : undefined;
    }

    getScrollValue(event) {
        return event ? event.deltaColumns : 0;
    }

    getDragValue(event) {
        return event.dragStartViewportColumn + event.columnsDelta;
    }

    getViewportBounds() {
        const bounds = super.getViewportBounds();
        return {
            start: bounds.start - this.labelsContainer.x,
            end: bounds.end - this.labelsContainer.x
        };
    }
}

export class RowsRenderer extends HeatmapAxisRenderer {
    /**
     *
     * @param {HeatmapViewOptions} options
     * @param {HeatmapInteractions} interactions
     * @param {HeatmapViewport} viewport
     * @param {LabelsManager} labelsManager
     */
    constructor(options, interactions, viewport, labelsManager) {
        super(
            options,
            interactions,
            viewport.rows,
            labelsManager,
            AxisVectors.rows.direction,
            AxisVectors.rows.normal
        );
    }

    // eslint-disable-next-line no-unused-vars
    test(event) {
        return event && (event.name !== EventTypes.drag || event.fitsRows());
    }

    getHoveredValue(event) {
        return event && event.row !== undefined
            ? Math.floor(event.row)
            : undefined;
    }

    getScrollValue(event) {
        return event ? event.deltaRows : 0;
    }

    getDragValue(event) {
        return event.dragStartViewportRow + event.rowsDelta;
    }

    getViewportBounds() {
        const bounds = super.getViewportBounds();
        return {
            start: bounds.start - this.labelsContainer.y,
            end: bounds.end - this.labelsContainer.y
        };
    }
}
