import * as PIXI from 'pixi.js-legacy';
import {Animated, Animation} from '../../../animation';
import {getSessionFlags, updateSessionFlags} from '../utilities/viewport-session-flags';
import HeatmapEventDispatcher from '../../../utilities/heatmap-event-dispatcher';
import colorProcessor from '../../../../utilities/colorProcessor';
import config from './config';
import events from '../../../utilities/events';

class HoveredElement extends Animated {
    /**
     *
     * @param {HeatmapDataItem} element
     */
    constructor(element) {
        super();
        this.session = {};
        /**
         *
         * @type {HeatmapDataItem}
         */
        this.element = element;
        this.hoveredRatio = 0;
        this.hovered = true;
    }

    startHover() {
        const animation = new Animation({
            from: this.hoveredRatio,
            to: 1,
            duration: 100
        })
            .onAnimation(this.emitAnimate.bind(this));
        this.startAnimation(animation);
    }

    startUnHover() {
        const animation = new Animation({
            from: this.hoveredRatio,
            to: 0,
            duration: 50
        })
            .onAnimation(this.emitAnimate.bind(this))
            .onFinish(this.emitDestroy.bind(this));
        this.startAnimation(animation);
    }

    get hovered() {
        return this._hovered;
    }

    set hovered(hovered) {
        if (this._hovered !== hovered) {
            this._hovered = hovered;
            if (hovered) {
                this.startHover();
            } else {
                this.startUnHover();
            }
        }
    }

    destroy() {
        super.destroy();
        this.element = undefined;
    }

    onAnimate(callback) {
        this.addEventListener(events.animation.tick, callback);
    }

    onDestroy(callback) {
        this.addEventListener(events.destroyed, callback);
    }

    emitAnimate(animation, value) {
        this.hoveredRatio = value;
        this.emit(events.animation.tick);
    }

    emitDestroy() {
        if (!this.hovered) {
            this.emit(events.destroyed);
        }
    }

    /**
     *
     * @param {PIXI.Graphics} graphics
     * @param {HeatmapViewport} viewport
     * @param {ColorScheme} colorScheme
     */
    render(graphics, viewport, colorScheme) {
        const {ratio} = this.session;
        this.session.ratio = this.hoveredRatio;
        if (this.element && graphics && colorScheme && viewport) {
            const {
                column,
                row,
                value
            } = this.element;
            const color = colorScheme.getColorForValue(value);
            if (color !== undefined) {
                const x1 = viewport.columns.getDevicePosition(column);
                const x2 = viewport.columns.getDevicePosition(column + 1);
                const xCenter = (x1 + x2) / 2.0;
                const y1 = viewport.rows.getDevicePosition(row);
                const y2 = viewport.rows.getDevicePosition(row + 1);
                const yCenter = (y1 + y2) / 2.0;
                const size = Math.max(config.minimumSize, viewport.scale.tickSize) +
                    2.0 * config.hoverSizeExtra * this.hoveredRatio;
                graphics
                    .beginFill(color, 1)
                    .lineStyle(
                        this.hovered ? 1 : 0,
                        this.hovered ? colorProcessor.darkenColor(color) : 0,
                        this.hovered ? 1 : 0,
                    )
                    .drawRect(
                        Math.floor(xCenter - size / 2.0),
                        Math.floor(yCenter - size / 2.0),
                        Math.ceil(size),
                        Math.ceil(size)
                    )
                    .endFill();
            }
        }
        return ratio !== this.session.ratio;
    }
}

export default class HoveredRenderer extends HeatmapEventDispatcher{
    /**
     *
     * @param {HeatmapViewport} viewport
     * @param {ColorScheme} colorScheme
     */
    constructor(viewport, colorScheme) {
        super();
        this.session = {};
        this.graphics = new PIXI.Graphics();
        this.viewport = viewport;
        this.colorScheme = colorScheme;
        /**
         *
         * @type {HoveredElement[]}
         */
        this.animatedElements = [];
        this.colorSchemeChangedCallback = this.colorSchemeChanged.bind(this);
        if (this.colorScheme) {
            this.colorScheme.onChanged(this.colorSchemeChangedCallback);
        }
        this._hovered = undefined;
    }

    destroy() {
        super.destroy();
        this.reset();
        if (this.colorScheme) {
            this.colorScheme.removeEventListeners(this.colorSchemeChangedCallback);
        }
        if (this.graphics) {
            this.graphics.clear();
            this.graphics.destroy();
            this.graphics = undefined;
        }
        this.graphics = undefined;
        this.viewport = undefined;
        this.colorScheme = undefined;
    }

    get hovered() {
        return this._hovered;
    }

    set hovered(hovered) {
        if (this._hovered !== hovered) {
            this._hovered = hovered;
            this.manageAnimations();
        }
    }

    manageAnimations() {
        this.animatedElements.forEach(element => {
            element.hovered = element.element === this.hovered;
        });
        let [hovered] = this.animatedElements.filter(o => o.hovered);
        if (!hovered && this.hovered) {
            hovered = new HoveredElement(this.hovered);
            hovered.onAnimate(() => {
                this._changed = true;
                this.requestRender();
            });
            hovered.onDestroy(e => {
                this.animatedElements = this.animatedElements.filter(o => o !== e);
                e.destroy();
                this._changed = true;
                this.requestRender();
            });
            this._changed = true;
            this.animatedElements.push(hovered);
        }
        this.requestRender();
    }

    colorSchemeChanged() {
        this._changed = true;
        this.requestRender();
    }

    onRequestRender(callback) {
        this.addEventListener(events.render.request, callback);
    }

    requestRender() {
        this.emit(events.render.request);
    }

    reset() {
        this.animatedElements.forEach(element => element.destroy());
        this.animatedElements = [];
        this._changed = true;
        this.session = {};
        this.requestRender();
    }

    getSessionFlags() {
        const viewportChanged = getSessionFlags(this.session, this.viewport);
        return this._changed || viewportChanged;
    }

    updateSessionFlags() {
        this._changed = false;
        this.session = updateSessionFlags(this.viewport);
    }

    render() {
        const changed = this.getSessionFlags();
        if (changed) {
            this.graphics.clear();
            this.animatedElements.forEach(element => {
                element.render(this.graphics, this.viewport, this.colorScheme);
            });
        }
        this.updateSessionFlags();
        return changed;
    }
}
