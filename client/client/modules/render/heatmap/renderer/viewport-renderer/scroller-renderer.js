import * as PIXI from 'pixi.js-legacy';
import {
    getPointProjectionInfo,
    makeSection,
    movePoint,
    moveSection,
    pointFitsSection
} from '../../utilities/vector-utilities';
import AxisVectors from '../../utilities/axis-vectors';
import InteractiveZone from '../../interactions/interactive-zone';
import config from './config';
import makeInitializable from '../../utilities/make-initializable';

class HeatmapScaleScroller extends InteractiveZone {
    /**
     *
     * @param {HeatmapAxis} axis
     * @param {{[x]: number, [y]: number}} [direction]
     * @param {{[x]: number, [y]: number}} [normal]
     */
    constructor(
        axis,
        direction = {},
        normal = {}
    ) {
        super({
            priority: InteractiveZone.Priorities.scaleScroller
        });
        makeInitializable(this);
        this.axis = axis;
        this.container = new PIXI.Container();
        const {
            x = 1,
            y = 0,
        } = direction;
        const {
            x: xn = 0,
            y: yn = 1,
        } = normal;
        this.direction = {x, y};
        this.normal = {x: xn, y: yn};
        this.directionRadians = Math.atan2(this.direction.y, this.direction.x);
        this.oppositeDirectionRadians = Math.atan2(-this.direction.y, -this.direction.x);
        this._scrollerHovered = false;
        this._scrollerDragging = false;
        this._initialized = false;
        this.clearRenderSession();
        this.initialize();
    }

    get scrollerDragging() {
        return this._scrollerDragging;
    }

    get scrollerHovered() {
        return this._scrollerHovered || this.scrollerDragging;
    }

    get size() {
        return config.scroller.height + config.scroller.margin * 2.0;
    }

    destroy() {
        if (this.container) {
            this.container.destroy();
        }
        super.destroy();
    }

    initialize() {
        this.container.removeChildren();
        this.scroller = new PIXI.Graphics();
        this.container.addChild(this.scroller);
        this.clearRenderSession();
        this.render();
        this.initialized = true;
    }

    clearRenderSession() {
        this.session = {};
    }

    /**
     *
     * @param {InteractiveZoneEvent} event
     * @param {{global: boolean, local: boolean}} options
     * @return {boolean}
     */
    testScrollerHover(event, options = {}) {
        if (!this.initialized) {
            return false;
        }
        const {
            global: testGlobal = true,
            local: testLocal = false
        } = options || {};
        const {
            global,
            local
        } = this.getScrollerParameters();
        const margin = config.scroller.hoverHeight;
        const fitsGlobal = !testGlobal || pointFitsSection(event, global, margin);
        const fitsLocal = !testLocal || pointFitsSection(event, local, margin);
        return fitsGlobal && fitsLocal;
    }

    /**
     *
     * @param {InteractiveZoneEvent} event
     */
    test(event) {
        return this.testScrollerHover(event);
    }

    onHover(event) {
        const scrollerHovered = event
            ? this.testScrollerHover(event)
            : false;
        if (scrollerHovered !== this._scrollerHovered) {
            this._scrollerHovered = scrollerHovered;
            this.requestRender();
        }
        if (scrollerHovered) {
            event.stopImmediatePropagation();
        }
    }

    shouldDrag(event) {
        if (this.testScrollerHover(event)) {
            event.stopImmediatePropagation();
        }
        return this.testScrollerHover(event, {global: false, local: true});
    }

    onDragStart(event) {
        event.stopImmediatePropagation();
        if (!this.scrollerDragging) {
            this._scrollerDragging = true;
            this.requestRender();
        }
    }

    /**
     * Gets new axis center
     * @param {InteractiveZoneDragEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    getDragValue(event) {
        if (this.direction.x !== 0) {
            return event
                ? (event.dragStartViewportColumn - this.direction.x * this.getGlobalScaleDimension(event.xDelta))
                : this.axis.center;
        }
        return event
            ? (event.dragStartViewportRow - this.direction.y * this.getGlobalScaleDimension(event.yDelta))
            : this.axis.center;
    }

    onDrag(event) {
        super.onDrag(event);
        this.axis.move(
            {
                center: this.getDragValue(event)
            },
            false
        );
    }

    onDragEnd(event) {
        event.stopImmediatePropagation();
        if (this.scrollerDragging) {
            this._scrollerDragging = false;
            this.requestRender();
        }
    }

    onClick(event) {
        super.onClick(event);
        if (
            this.testScrollerHover(event, {global: true, local: false}) &&
            !this.testScrollerHover(event, {global: false, local: true})
        ) {
            const {global} = this.getScrollerParameters();
            const {projectionDistanceFromStart} = getPointProjectionInfo(event, global);
            this.axis.move({
                center: this.getGlobalValue(projectionDistanceFromStart)
            });
        }
    }

    getGlobalDimension(axisDimension, offset = 0) {
        if (this.axis.invalid || this.axis.size <= 0) {
            return 0;
        }
        const deviceSize = this.axis.scale.getDeviceDimension(this.axis.range) - offset;
        return axisDimension / this.axis.size * deviceSize;
    }

    getGlobalPosition(value, margin = 0) {
        if (this.axis.invalid || this.axis.size <= 0) {
            return 0;
        }
        const deviceStart = this.axis.getDevicePosition(this.axis.start) + margin;
        return deviceStart + this.getGlobalDimension(value, 2.0 * margin);
    }

    getGlobalScaleDimension(deviceDimension) {
        if (this.axis.invalid || this.axis.size <= 0) {
            return 0;
        }
        return deviceDimension
            / this.axis.scale.getDeviceDimension(this.axis.range)
            * this.axis.size;
    }

    getGlobalValue(position) {
        return this.getGlobalScaleDimension(
            position - this.axis.getDevicePosition(this.axis.start)
        );
    }

    getScrollerParameters() {
        const createSection = (start, end) => {
            const actual = this.getGlobalDimension(end - start);
            const size = Math.max(
                config.scroller.minimumSize,
                actual
            );
            const diff = Math.max(0, size - actual);
            const center = this.getGlobalPosition((start + end) / 2.0, diff / 2.0);
            const startPx = center - size / 2.0;
            const endPx = center + size / 2.0;
            return moveSection(
                makeSection(
                    this.direction,
                    startPx,
                    endPx
                ),
                this.normal,
                config.scroller.margin + config.scroller.height / 2.0
            );
        };
        return {
            global: createSection(0, this.axis.size),
            local: createSection(this.axis.start, this.axis.end)
        };
    }

    renderScroller() {
        const {
            global,
            local
        } = this.getScrollerParameters();
        this.scroller.clear();
        const renderBar = (barConfig, section) => {
            const {
                a: from,
                b: to
            } = section;
            const halfHeight = config.scroller.height / 2.0;
            const a = movePoint(from, this.normal, halfHeight);
            const b = movePoint(to, this.normal, halfHeight);
            const c = movePoint(to, this.normal, -halfHeight);
            const d = movePoint(from, this.normal, -halfHeight);
            this.scroller
                .beginFill(barConfig.fill, barConfig.alpha)
                .moveTo(
                    a.x,
                    a.y
                )
                .lineTo(
                    b.x,
                    b.y
                )
                .lineTo(
                    c.x,
                    c.y
                )
                .lineTo(
                    d.x,
                    d.y
                )
                .endFill();
        };
        renderBar(
            config.scroller.background,
            global
        );
        renderBar(
            this.scrollerHovered
                ? config.scroller.hovered
                : config.scroller.range,
            local
        );
    }

    render() {
        if (!this.initialized) {
            return false;
        }
        const scaleChanged = this.session.scale !== this.axis.scale.tickSize;
        const positionChanged = this.session.center !== this.axis.center ||
            this.session.deviceSize !== this.axis.deviceSize;
        const scrollerChanged = positionChanged ||
            this.session.hovered !== this.scrollerHovered;
        if (positionChanged || scrollerChanged || scaleChanged) {
            this.renderScroller();
        }
        this.session.scale = this.axis.scale.tickSize;
        this.session.center = this.axis.center;
        this.session.deviceSize = this.axis.deviceSize;
        this.session.hovered = this.scrollerHovered;
        return scaleChanged ||
            positionChanged ||
            scrollerChanged;
    }
}

export default HeatmapScaleScroller;
