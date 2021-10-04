import * as PIXI from 'pixi.js-legacy';
import AxisLabel from './utilities/axis-label';
import AxisVectors from './axis-vectors';
import {EventTypes} from '../../interactions/events';
import InteractiveZone from '../../interactions/interactive-zone';
import cancellablePromise from '../data-renderer/utilities/cancellable-promise';
import config from './config';
import events from '../../utilities/events';
import makeInitializable from '../../utilities/make-initializable';
import {movePoint} from './utilities/vector-utilities';

class HeatmapAxisRenderer extends InteractiveZone {
    /**
     *
     * @param {HeatmapAxis} axis
     * @param {LabelsManager} labelsManager
     * @param {{[x]: number, [y]: number}} [direction]
     * @param {{[x]: number, [y]: number}} [normal]
     */
    constructor(
        axis,
        labelsManager,
        direction = {},
        normal = {}
    ) {
        super({
            priority: InteractiveZone.Priorities.scale
        });
        makeInitializable(this);
        this.axis = axis;
        this.labelsManager = labelsManager;
        this.container = new PIXI.Container();
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
        if (this.container) {
            this.container.destroy();
        }
        if (this.ticks) {
            this.ticks.forEach(tick => tick.destroy());
            this.ticks = [];
        }
        this.labelsManager = undefined;
        super.destroy();
    }

    get axisSize() {
        if (Number.isNaN(Number(this._labelsMaxSize))) {
            return config.maxAxisSize;
        }
        return Math.min(
            config.maxAxisSize,
            Number(this._labelsMaxSize)
        );
    }

    get labelFitsTick () {
        return !Number.isNaN(Number(this._labelsMaxSize)) &&
            this._labelsMaxSize <= this.axis.scale.tickSize;
    }

    get labelExtraRotation () {
        if (!Number.isNaN(Number(this._labelsMaxSize)) && this._labelsMaxSize > config.maxAxisSize) {
            return Math.min(Math.PI / 4.0, Math.acos(config.maxAxisSize / this._labelsMaxSize));
        }
        return 0;
    }

    onAxisItemClick(callback) {
        this.addEventListener(events.click, callback);
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
        this.cancelInitialization = cancellablePromise(
            (isCancelledFn) => new Promise((resolve) => AxisLabel.initializeTicks(isCancelledFn, options, labels)
                .then((ticks) => this.ticks = ticks)
                .then(this.calculateAxisSize.bind(this))
                .then(() => {
                    if (!isCancelledFn()) {
                        this._hoveredTick = undefined;
                        this.container.removeChildren();
                        this.labelsContainer = new PIXI.Container();
                        this.container.addChild(this.labelsContainer);
                        this.ticks.forEach(tick => {
                            this.labelsContainer.addChild(tick.container);
                        });
                        this.clearRenderSession();                        requestAnimationFrame(() => {
                            this.render();
                            this.initialized = true;
                            this.requestRender();
                            resolve();
                        });
                    } else {
                        resolve();
                    }
                })
            ),
            this.cancelInitialization,
        );
    }

    calculateAxisSize () {
        this._labelsMaxSize = Math.max(0, ...this.ticks.map(tick => tick.size));
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
        const ranges = [];
        return this.ticks.map(tick => tick.updateVisibility(ranges, viewportBounds, this.hoveredTick))
            .filter(Boolean)
            .length > 0;
    }

    translate() {
        const shift = this.axis.getDevicePosition(0);
        this.labelsContainer.x = this.direction.x * shift;
        this.labelsContainer.y = this.direction.y * shift;
    }

    render() {
        if (!this.initialized) {
            return false;
        }
        const scaleChanged = this.session.scale !== this.axis.scale.tickSize;
        const positionChanged = scaleChanged ||
            this.session.center !== this.axis.center ||
            this.session.deviceSize !== this.axis.deviceSize;
        const hoveredTickValue = this.hoveredTick ? this.hoveredTick.value : undefined;
        const hoverChanged = this.session.hovered !== hoveredTickValue;
        const visibilityChanged = (scaleChanged || hoverChanged) && !this.axis.isAnimating;
        let changed = false;
        if (scaleChanged) {
            changed = this.updateTicksPositions() || changed;
        }
        if (positionChanged || hoverChanged || visibilityChanged) {
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
        return scaleChanged ||
            positionChanged ||
            hoverChanged ||
            visibilityChanged ||
            changed;
    }
}

export class ColumnsRenderer extends HeatmapAxisRenderer {
    constructor(viewport, labelsManager) {
        super(
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
    constructor(viewport, labelsManager) {
        super(
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
