import * as PIXI from 'pixi.js-legacy';
import {extendRectangleByMargin, getRotatedRectangleBounds, rotateRectangle} from './vector-utilities';
import {HeatmapNavigationType} from '../../../navigation';
import config from '../config';
import labelsFormatter from './labels-formatter';
import {linearDimensionsConflict} from '../../../../utilities';

const DEBUG = false;

/**
 * @typedef {Object} AxisLabelOptions
 * @property {HeatmapAnnotatedIndex} annotatedIndex
 * @property {LabelsManager} labelsManager
 * @property {HeatmapAxis} axis
 * @property {Vector} direction
 * @property {Vector} normal
 * @property {boolean} showAnnotations
 * @property {number} value
 */

class AxisLabel {
    /**
     *
     * @param {function: boolean} isCancelledFn
     * @param {AxisLabelOptions} options
     * @param {HeatmapAnnotatedIndex[]} ticks
     * @returns {Promise<AxisLabel[]>}
     */
    static initializeTicks(isCancelledFn = (() => false), options = {}, ticks = []) {
        const {
            labelsManager
        } = options;
        if (!labelsManager || ticks.length === 0 || isCancelledFn()) {
            return Promise.resolve([]);
        }
        const maxIterationsPerFrame = 50;
        return new Promise((resolve) => {
            const result = [];
            const _iterate = (index = 0) => {
                const nextIterationIndex = index + maxIterationsPerFrame;
                for (let i = index; i < nextIterationIndex && i < ticks.length; i++) {
                    if (isCancelledFn()) {
                        break;
                    }
                    result.push(new AxisLabel({
                        ...options,
                        annotatedIndex: ticks[i],
                        value: i
                    }));
                }
                if (nextIterationIndex >= ticks.length || isCancelledFn()) {
                    resolve(result);
                } else {
                    requestAnimationFrame(() => _iterate(nextIterationIndex));
                }
            };
            requestAnimationFrame(() => _iterate());
        });
    }
    /**
     *
     * @param {AxisLabelOptions} options
     */
    constructor(options = {}) {
        const {
            annotatedIndex,
            labelsManager,
            axis,
            direction,
            normal,
            showAnnotations,
            value = 0
        } = options;
        /**
         *
         * @type {PIXI.Container}
         */
        this.container = new PIXI.Container();
        /**
         * Label text
         * @type {string}
         */
        this.text = showAnnotations && annotatedIndex.annotation
            ? `${annotatedIndex.name || ''} - ${annotatedIndex.annotation}`
            : (annotatedIndex.name || '');
        /**
         *
         * @type {HeatmapAnnotatedIndex}
         */
        this.annotatedIndex = annotatedIndex;
        /**
         * Label text lines
         * @type {string[]}
         */
        this.lines = labelsFormatter(this.text);
        /**
         * Label text with line breaks
         * @type {string}
         */
        this.formattedText = this.lines.join('\n');
        /**
         * Label text size
         * @type {number}
         */
        this.length = this.text.length;
        /**
         * Longest line size
         * @type {number}
         */
        this.formattedLength = Math.max(...this.lines.map(line => line.length), 0);
        /**
         *
         * @type {HeatmapAxis}
         */
        this.axis = axis;
        /**
         *
         * @type {boolean}
         */
        this.visible = false;
        /**
         *
         * @type {boolean}
         */
        this.hovered = false;
        /**
         * Axis direction vector
         * @type {Vector}
         */
        this.direction = direction;
        /**
         * Axis normal vector
         * @type {Vector}
         */
        this.normal = normal;
        /**
         * Direction angle (radians)
         * @type {number}
         */
        this.directionRadians = Math.atan2(this.direction.y, this.direction.x);
        /**
         * Normal angle (radians)
         * @type {number}
         */
        this.normalRadians = Math.atan2(this.normal.y, this.normal.x);
        const {x: ax = 0} = this.normal;
        const align = ax > 0 ? 'left' : 'right';
        /**
         * Label graphics
         * @type {PIXI.Text|PIXI.Sprite}
         */
        this.label = labelsManager.getLabel(this.formattedText, {align, ...config.label.font});
        /**
         * Hovered label graphics
         * @type {PIXI.Text|PIXI.Sprite}
         */
        this.hoveredLabel = labelsManager.getLabel(this.formattedText, {align, ...config.label.hoveredFont});
        /**
         * Label graphics size (width) respecting margin
         */
        this.container.addChild(this.label);
        this.container.addChild(this.hoveredLabel);
        if (
            annotatedIndex &&
            annotatedIndex.navigation !== HeatmapNavigationType.missing
        ) {
            this.container.interactive = true;
            this.container.buttonMode = true;
        }
        if (DEBUG) {
            this.debugGraphics = new PIXI.Graphics();
            this.container.addChild(this.debugGraphics);
        }
        this.size = this.hoveredLabel.width +
            config.label.margin * 2.0 +
            config.scroller.height +
            config.scroller.margin * 2.0;
        this.value = value;
        this.anchor = {x: ax >= 0 ? 0 : 1, y: 0.5};
        this.start = 0;
        this.end = 0;
        this.built = false;
        this.changed = true;
    }

    destroy() {
        if (this.container) {
            this.container.removeChildren();
            if (this.container.parent) {
                this.container.parent.removeChild(this.container);
            }
            this.container = undefined;
        }
        if (this.label) {
            this.label = undefined;
        }
        if (this.hoveredLabel) {
            this.hoveredLabel = undefined;
        }
        this.axis = undefined;
    }

    get hovered() {
        return this._hovered;
    }

    set hovered(hovered) {
        if (this._hovered !== hovered) {
            this._hovered = hovered;
            this.changed = true;
        }
    }

    get faded() {
        return this._faded;
    }

    set faded(faded) {
        if (this._faded !== faded) {
            this._faded = faded;
            this.changed = true;
        }
    }

    get visible() {
        return this._visible;
    }

    set visible(visible) {
        if (this._visible !== visible) {
            this._visible = visible;
            this.changed = true;
        }
    }

    getTooltip() {
        return [
            [this.annotatedIndex.name],
            this.annotatedIndex.annotation ? [this.annotatedIndex.annotation] : undefined
        ].filter(Boolean);
    }

    /**
     * Transforms label's bounds to start-end region
     * @property {PIXI.Text|PIXI.Sprite} [label]
     * @property {number} [rotation]
     * @returns {{start: number, end: number}}
     */
    getLabelBounds(label = this.label, rotation = label.rotation) {
        const bounds = getRotatedRectangleBounds(
            extendRectangleByMargin(
                label ? label.getLocalBounds() : undefined,
                config.label.margin
            ),
            rotation
        );
        return {
            start: bounds && label
                ? (
                    (bounds.x + label.x) * this.direction.x +
                    (bounds.y + label.y) * this.direction.y
                )
                : Infinity,
            end: bounds && label
                ? (
                    (bounds.x + label.x + bounds.width) * this.direction.x +
                    (bounds.y + label.y + bounds.height) * this.direction.y
                )
                : Infinity
        };
    }

    /**
     *
     * @param {Point2D} point
     */
    test(point) {
        const bounds = this.hovered ? this.realHoveredBounds : this.realBounds;
        if (bounds && point) {
            return bounds.contains(point.x, point.y);
        }
        return false;
    }

    /**
     * Updates label's rotation and position
     * @param {Object} [options]
     * @param {number} [options.extraRotation=0]
     * @param {boolean} [options.fitsTick=false]
     * @returns {boolean} true if graphics changed
     */
    updatePosition(options = {}) {
        if (!this.axis || this.axis.invalid) {
            return false;
        }
        const {
            extraRotation = 0,
            fitsTick = false
        } = options;
        const shift = this.axis.scale.getDeviceDimension(this.value + 0.5);
        const margin = config.label.margin * 2.0 +
            config.scroller.height +
            config.scroller.margin * 2.0;
        const center = {
            x: shift * this.direction.x + this.normal.x * margin,
            y: shift * this.direction.y + this.normal.y * margin
        };
        const getConfig = (label) => ({
            x: label.x,
            y: label.y,
            rotation: label.rotation,
            anchor: label.anchor
        });
        const previous = getConfig(this.label);
        const previousHovered = getConfig(this.hoveredLabel);
        let labelMainAngle = this.normalRadians + (this.normal.x < 0 ? Math.PI : 0);
        if (fitsTick && Math.abs(this.direction.x) > 0) {
            labelMainAngle = this.directionRadians;
            this.label.anchor.set(0.5, 1);
            this.label.rotation = labelMainAngle;
            this.hoveredLabel.anchor.set(0.5, 1);
            this.hoveredLabel.rotation = labelMainAngle;
            this.start = shift - this.label.width / 2.0;
            this.end = shift + this.label.width / 2.0;
        } else {
            this.label.anchor.set(this.anchor.x, this.anchor.y);
            this.label.rotation = labelMainAngle + extraRotation;
            this.hoveredLabel.anchor.set(this.anchor.x, this.anchor.y);
            this.hoveredLabel.rotation = labelMainAngle + extraRotation;
            this.start = shift - this.label.height / 2.0 - config.label.margin;
            this.end = shift + this.label.height / 2.0 + config.label.margin;
        }
        this.label.x = Math.round(center.x);
        this.label.y = Math.round(center.y);
        this.hoveredLabel.x = Math.round(center.x);
        this.hoveredLabel.y = Math.round(center.y);
        const changed = (prev, cur) => prev.x !== cur.x ||
            prev.y !== cur.y ||
            prev.rotation !== cur.rotation ||
            prev.anchor.x !== cur.anchor.x ||
            prev.anchor.y !== cur.anchor.y;
        const getRealBounds = (bounds, rotation) => {
            const realBounds = rotateRectangle(bounds, rotation);
            return new PIXI.Polygon(
                realBounds.a.x + this.label.x,
                realBounds.a.y + this.label.y,
                realBounds.b.x + this.label.x,
                realBounds.b.y + this.label.y,
                realBounds.c.x + this.label.x,
                realBounds.c.y + this.label.y,
                realBounds.d.x + this.label.x,
                realBounds.d.y + this.label.y
            );
        };
        this.realBounds = getRealBounds(
            extendRectangleByMargin(this.label.getLocalBounds(), config.label.margin),
            this.label.rotation
        );
        this.realHoveredBounds = getRealBounds(
            extendRectangleByMargin(this.hoveredLabel.getLocalBounds(), config.label.margin),
            this.hoveredLabel.rotation
        );
        this.axisBounds = this.getLabelBounds(this.label, labelMainAngle);
        this.axisHoveredBounds = this.getLabelBounds(this.hoveredLabel, labelMainAngle);
        if (changed(previous, this.label) || changed(previousHovered, this.hoveredLabel)) {
            this.changed = true;
        }
        this.built = true;
        return this.changed;
    }

    /**
     *
     * @param {{end: number}} lastVisibleTickInfo
     * @param {{start: number, end: number}} viewportRange
     * @param {AxisLabel} [hovered]
     * @returns {boolean} true if graphics changed
     */
    updateVisibility(lastVisibleTickInfo = {}, viewportRange, hovered) {
        if (!this.axis || this.axis.invalid || !this.built) {
            return false;
        }
        const range = this.axisBounds;
        const {
            end
        } = lastVisibleTickInfo;
        this.visible = end === undefined ||
            !linearDimensionsConflict(
                range.start,
                range.end,
                -Infinity,
                end
            );
        if (this.visible) {
            lastVisibleTickInfo.end = range.end;
        }
        if (hovered === this) {
            this.faded = false;
            this.visible = true;
        } else {
            this.faded = hovered && hovered.built &&
                linearDimensionsConflict(
                    range.start,
                    range.end,
                    hovered.axisHoveredBounds.start,
                    hovered.axisHoveredBounds.end
                );
        }
        const center = (range.start + range.end) / 2.0;
        this.visible = this.visible && viewportRange.start <= center && viewportRange.end >= center;
        return this.changed;
    }

    /**
     *
     * @returns {boolean} true if graphics changed
     */
    render() {
        const changed = this.changed;
        this.label.visible = this.visible && !this.hovered;
        this.hoveredLabel.visible = this.visible && this.hovered;
        if (this.faded) {
            this.label.alpha = config.label.fade;
        } else {
            this.label.alpha = 1.0;
        }
        if (DEBUG && this.debugGraphics) {
            this.debugGraphics.clear();
            const bounds = this.hovered ? this.axisHoveredBounds : this.axisBounds;
            const realBounds = this.hovered ? this.realHoveredBounds : this.realBounds;
            this.debugGraphics
                .lineStyle(1, this.hovered ? 0xff0000 : 0x00ff00, this.visible ? 1.0 : 0.25)
                .drawRect(
                    bounds.start * this.direction.x,
                    bounds.start * this.direction.y,
                    (bounds.end - bounds.start) * this.direction.x + 10 * this.normal.x,
                    (bounds.end - bounds.start) * this.direction.y + 10 * this.normal.y
                );
            if (realBounds) {
                this.debugGraphics
                    .lineStyle(1, this.hovered ? 0xff00ff : 0x0000ff, this.visible ? 1.0 : 0.25)
                    .drawPolygon(realBounds);
            }
        }
        this.changed = false;
        return changed;
    }
}

export default AxisLabel;
