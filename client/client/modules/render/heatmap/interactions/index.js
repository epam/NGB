import tooltipFactory from '../../core/track/tooltip';
import {DefaultInteractiveZone} from './interactive-zone';
import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import buildCallbacks from './build-callbacks';
import events from '../utilities/events';

const REQUEST_RENDER = Symbol('Render requested');

class HeatmapInteractions extends HeatmapEventDispatcher {
    constructor(node, viewport) {
        super();
        this.viewport = viewport;
        this.node = node;
        this._userInteracted = false;
        this.onInteractionOccurs = () => this._userInteracted = true;
        if (node) {
            this.tooltip = tooltipFactory(node);
        }
        this.defaultZone = new DefaultInteractiveZone(viewport);
        /**
         * @type {InteractiveZone[]}
         */
        this.interactiveZones = [];
        this.handleRequestRender = this.requestRender.bind(this);
        /**
         *
         * @param {HeatmapEventDispatcher} [target]
         * @param {Object} [payload]
         * @param {InteractiveZoneEvent} [payload.event]
         * @param {*[]} [payload.content]
         */
        this.handleShowTooltipEvent = (target, payload) => {
            if (payload && payload.event && payload.content) {
                this.showTooltip(payload.event, payload.content);
            } else {
                this.hideTooltip();
            }
        };
        this.registerInteractiveZone(this.defaultZone);
        this.makeInteractive();
        this.checkRenderRequest();
    }

    destroy() {
        super.destroy();
        if (this.tooltip) {
            this.tooltip.hide();
            this.tooltip = undefined;
        }
        this.clearRenderRequestTimeout();
        this.interactiveZones.forEach(zone => zone.destroy());
        this.interactiveZones = [];
        this.node = undefined;
        this.viewport = undefined;
        if (typeof this.clearHandlers === 'function') {
            this.clearHandlers();
        }
    }

    get userInteracted() {
        return this._userInteracted;
    }

    clearRenderRequestTimeout() {
        if (this.checkRenderRequestTimeout) {
            cancelAnimationFrame(this.checkRenderRequestTimeout);
            this.checkRenderRequestTimeout = undefined;
        }
    }

    checkRenderRequest() {
        if (this[REQUEST_RENDER]) {
            this[REQUEST_RENDER] = false;
            this.emit(events.render.request);
        }
        this.checkRenderRequestTimeout = requestAnimationFrame(this.checkRenderRequest.bind(this));
    }

    onRender(callback) {
        this.addEventListener(events.render.request, callback);
    }

    requestRender() {
        this[REQUEST_RENDER] = true;
    }

    /**
     * Registers interactive zone
     * @param {InteractiveZone} zone
     */
    registerInteractiveZone(zone) {
        if (zone && !this.interactiveZones.includes(zone)) {
            this.interactiveZones.push(zone);
            zone.addEventListener(events.tooltip.show, this.handleShowTooltipEvent);
            zone.addEventListener(events.tooltip.hide, this.handleShowTooltipEvent);
            zone.addEventListener(events.render.request, this.handleRequestRender);
            zone.addEventListener(events.interaction, this.onInteractionOccurs);
            this.interactiveZones.sort((a, b) => b.priority - a.priority);
        }
    }

    unregisterInteractiveZone(zone) {
        const index = this.interactiveZones.indexOf(zone);
        if (index >= 0) {
            this.interactiveZones[index].removeEventListeners(
                this.handleShowTooltipEvent,
                this.handleRequestRender,
                this.onInteractionOccurs
            );
            this.interactiveZones.splice(index, 1);
            this.interactiveZones.sort((a, b) => b.priority - a.priority);
        }
    }

    /**
     * Show tooltip
     * @param {InteractiveZoneEvent} event
     * @param {*[]} tooltip
     */
    showTooltip(event, tooltip) {
        if (!event || !tooltip || !tooltip.length) {
            this.hideTooltip();
        } else if (this.tooltip) {
            this.tooltip.setContent(tooltip);
            this.tooltip.show({x: event.globalX, y: event.globalY});
            this.tooltip.correctPosition({x: event.globalX, y: event.globalY});
        }
    }

    hideTooltip() {
        if (this.tooltip) {
            this.tooltip.hide();
        }
    }

    /**
     * Adds mouse event handlers for node
     */
    makeInteractive () {
        if (!this.viewport || !this.node) {
            return undefined;
        }
        /**
         *
         * @param {InteractiveZoneEvent} event
         * @param {InteractiveZone[]} zones
         */
        const onHover = (event, zones = []) => {
            this.interactiveZones.forEach(zone => {
                const hovered = event &&
                    !event.immediatePropagationStopped &&
                    zones.includes(zone);
                zone.onHover(hovered ? event : undefined);
            });
        };
        /**
         *
         * @param {InteractiveZoneEvent} e
         * @returns {InteractiveZone[]}
         */
        const check = (e) => this.interactiveZones
            .filter(zone => zone.test(e));
        const {
            mouseOver,
            mouseOut,
            mouseDown,
            mouseMove,
            mouseUp,
            click,
            doubleClick,
            mouseWheel
        } = buildCallbacks(
            this.viewport,
            onHover,
            check
        );
        window.jQuery(this.node)
            .on('mouseenter', mouseOver)
            .on('mouseleave', mouseOut)
            .on('mousedown', mouseDown)
            .on('touchstart', mouseDown)
            .on('click', click)
            .on('dblclick', doubleClick)
            .on('mousewheel', mouseWheel);
        window.jQuery(window)
            .on('mousemove', mouseMove)
            .on('touchmove', mouseMove)
            .on('mouseup', mouseUp)
            .on('mouseupoutside', mouseUp)
            .on('touchend', mouseUp)
            .on('touchendoutside', mouseUp);
        function off() {
            window.jQuery(this.node)
                .off('mouseenter', mouseOver)
                .off('mouseleave', mouseOut)
                .off('mousedown', mouseDown)
                .off('touchstart', mouseDown)
                .off('click', click)
                .off('dblclick', doubleClick)
                .off('mousewheel', mouseWheel);
            window.jQuery(window)
                .off('mousemove', mouseMove)
                .off('touchmove', mouseMove)
                .off('mouseup', mouseUp)
                .off('mouseupoutside', mouseUp)
                .off('touchend', mouseUp)
                .off('touchendoutside', mouseUp);
        }
        this.clearHandlers = off.bind(this);
    }
}

export default HeatmapInteractions;
