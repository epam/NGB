import * as PIXI from 'pixi.js-legacy';
import AxisVectors from './axis-vectors';
import InteractiveZone from '../../interactions/interactive-zone';
import config from './config';
import labelsFormatter from './labels-formatter';
import labelsInitializer from './labels-initializer';
import {linearDimensionsConflict} from '../../../utilities';
import makeInitializable from '../../utilities/make-initializable';

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
        const {
            x = 1,
            y = 0,
        } = direction;
        const {x: ax = 0} = normal;
        this.direction = {x, y};
        this.normal = normal;
        this.directionRadians = Math.atan2(this.direction.y, this.direction.x);
        this.normalRadians = Math.atan2(this.normal.y, this.normal.x);
        this.tickAnchor = {
            x: ax >= 0 ? 0 : 1,
            y: 0.5
        };
        this.tickAlign = ax > 0 ? 'left' : 'right';
        this._labelSprites = [];
        this._labels = [];
        /**
         * @type {PIXI.Sprite}
         * @private
         */
        this._hoveredLabel = undefined;
        this._labelVisibilityInfo = [];
        this._hoveredValue = undefined;
        this.clearRenderSession();
        this.initialize();
    }

    get hoveredValue() {
        return this._hoveredValue;
    }

    destroy() {
        if (this.container) {
            this.container.destroy();
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

    /**
     * Initializes axis labels
     * @param {Array<string>} [labels = []]
     */
    initialize(labels = []) {
        if (labels.length === 0) {
            return;
        }
        this._labels = labels.map(label => {
            const lines = labelsFormatter(label);
            return {
                label,
                lines,
                formatted: lines.join('\n'),
                length: label.length,
                formattedLength: Math.max(...lines.map(line => line.length), 0)
            };
        });
        const formattedLabels = this._labels.map(o => o.formatted);
        labelsInitializer(this.labelsManager, {...config.label.font, align: this.tickAlign}, formattedLabels)
            .then((_labels) => {
                this._labelSprites = _labels;
                this._labelVisibilityInfo = formattedLabels.map((label) => ({
                    visible: true,
                    label
                }));
            })
            .then(this.calculateAxisSize.bind(this))
            .then(() => {
                this.container.removeChildren();
                this.labelsContainer = new PIXI.Container();
                this.container.addChild(this.labelsContainer);
                this._labelSprites.forEach(label => {
                    label.visible = false;
                    this.labelsContainer.addChild(label);
                });
                this.clearRenderSession();
                requestAnimationFrame(() => {
                    this.render();
                    this.initialized = true;
                });
            });
    }

    calculateAxisSize () {
        const [longest] = this._labels
            .slice()
            .sort((a, b) => b.formattedLength - a.formattedLength);
        const texture = this.labelsManager.getLabel(
            longest.formatted,
            {...config.label.hoveredFont, align: this.tickAlign}
        );
        this._labelsMaxSize = texture.width +
            config.label.margin * 2.0 +
            config.scroller.height +
            config.scroller.margin * 2.0;
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

    onHover(event) {
        const value = this.getHoveredValue(event);
        if (value !== this._hoveredValue) {
            this._hoveredValue = value;
            this.requestRender();
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

    rebuildLabels() {
        for (let l = 0; l < this._labelSprites.length; l += 1) {
            const shift = this.axis.scale.getDeviceDimension(l + 0.5);
            const margin = config.label.margin * 2.0 +
                config.scroller.height +
                config.scroller.margin * 2.0;
            const center = {
                x: shift * this.direction.x + this.normal.x * margin,
                y: shift * this.direction.y + this.normal.y * margin
            };
            if (!this._labelVisibilityInfo[l]) {
                this._labelVisibilityInfo.push({visible: true});
            }
            if (this.labelFitsTick && Math.abs(this.direction.x) > 0) {
                this._labelSprites[l].anchor.set(0.5, 1);
                this._labelSprites[l].rotation = this.directionRadians;
                this._labelVisibilityInfo[l].start = shift
                    - this._labelSprites[l].width / 2.0;
                this._labelVisibilityInfo[l].end = shift
                    + this._labelSprites[l].width / 2.0;
            } else {
                this._labelSprites[l].anchor.set(this.tickAnchor.x, this.tickAnchor.y);
                this._labelSprites[l].rotation = this.normalRadians +
                    (this.normal.x < 0 ? Math.PI : 0) +
                    this.labelExtraRotation;
                this._labelVisibilityInfo[l].start = shift
                    - this._labelSprites[l].height / 2.0
                    - config.label.margin;
                this._labelVisibilityInfo[l].end = shift
                    + this._labelSprites[l].height / 2.0
                    + config.label.margin;
            }
            this._labelSprites[l].x = Math.round(center.x);
            this._labelSprites[l].y = Math.round(center.y);
            this._labelVisibilityInfo[l].anchor = this._labelSprites[l].anchor;
            this._labelVisibilityInfo[l].rotation = this._labelSprites[l].rotation;
            this._labelVisibilityInfo[l].x = this._labelSprites[l].x;
            this._labelVisibilityInfo[l].y = this._labelSprites[l].y;
        }
    }

    rebuildLabelsVisibility() {
        this._labelVisibilityInfo.forEach(info => {
            info.visible = false;
            info.hovered = false;
        });
        let hoveredRange;
        let previousOccupiedPosition = 0;
        if (this._hoveredLabel) {
            this._hoveredLabel.parent.removeChild(this._hoveredLabel);
            this._hoveredLabel.destroy();
            this._hoveredLabel = undefined;
        }
        if (this.hoveredValue !== undefined && this._labelVisibilityInfo[this.hoveredValue]) {
            this._labelVisibilityInfo[this.hoveredValue].visible = false;
            this._labelVisibilityInfo[this.hoveredValue].hovered = true;
            this._labelVisibilityInfo[this.hoveredValue].faded = false;
            const {
                start,
                end,
                label,
                anchor,
                rotation,
                x,
                y
            } = this._labelVisibilityInfo[this.hoveredValue];
            hoveredRange = {start, end};
            if (label && this.labelsManager) {
                this._hoveredLabel = this.labelsManager.getLabel(
                    label,
                    {...config.label.hoveredFont, align: this.tickAlign}
                );
                this._hoveredLabel.anchor.set(anchor.x, anchor.y);
                this._hoveredLabel.rotation = rotation;
                this._hoveredLabel.x = x;
                this._hoveredLabel.y = y;
                this.labelsContainer.addChild(this._hoveredLabel);
            }
        }
        for (let l = 0; l < this._labelVisibilityInfo.length; l++) {
            const {
                start,
                end,
                hovered: labelHovered,
            } = this._labelVisibilityInfo[l];
            const notConflicts = previousOccupiedPosition < start;
            if (notConflicts) {
                previousOccupiedPosition = end;
            }
            if (labelHovered) {
                continue;
            }
            this._labelVisibilityInfo[l].visible = notConflicts;
            this._labelVisibilityInfo[l].faded = hoveredRange &&
                linearDimensionsConflict(hoveredRange.start, hoveredRange.end, start, end);
        }
    }

    translate() {
        const shift = this.axis.getDevicePosition(0);
        this.labelsContainer.x = this.direction.x * shift;
        this.labelsContainer.y = this.direction.y * shift;
        const valueVisible = (value) => this.axis.start <= (value + 0.5) &&
            (value - 0.5) <= this.axis.end;
        if (this._hoveredLabel && this.hoveredValue !== undefined) {
            this._hoveredLabel.visible = valueVisible(this.hoveredValue);
        }
        for (let l = 0; l < this._labelSprites.length; l += 1) {
            const {faded, visible} = this._labelVisibilityInfo[l];
            this._labelSprites[l].visible = visible && valueVisible(l);
            this._labelSprites[l].alpha = faded ? config.label.fade : 1;
        }
    }

    render() {
        if (!this.initialized) {
            return false;
        }
        const scaleChanged = this.session.scale !== this.axis.scale.tickSize;
        const positionChanged = scaleChanged ||
            this.session.center !== this.axis.center ||
            this.session.deviceSize !== this.axis.deviceSize;
        const hoverChanged = this.session.hovered !== this.hoveredValue;
        const visibilityChanged = (scaleChanged || hoverChanged) && !this.axis.isAnimating;
        if (scaleChanged) {
            this.rebuildLabels();
        }
        if (visibilityChanged) {
            this.rebuildLabelsVisibility();
        }
        if (positionChanged || hoverChanged || visibilityChanged) {
            this.translate();
        }
        this.session.scale = this.axis.scale.tickSize;
        this.session.center = this.axis.center;
        this.session.deviceSize = this.axis.deviceSize;
        this.session.hovered = this.hoveredValue;
        return scaleChanged ||
            positionChanged ||
            hoverChanged ||
            visibilityChanged;
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
        return event && event.fitsColumns();
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

    test(event) {
        return event && event.fitsRows();
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
}
