import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import events from '../utilities/events';

/**
 * @typedef {Object} InteractiveZoneOptions
 * @property {number} priority
 */

class InteractiveZone extends HeatmapEventDispatcher {
    static Priorities = {
        default: 1,
        colorScheme: 0,
        scale: 1,
        scaleScroller: 2,
        viewport: 2,
        data: 3
    };
    /**
     *
     * @param {InteractiveZone} [options]
     */
    constructor(options = {}) {
        super();
        const {priority = InteractiveZone.Priorities.default} = options;
        this.priority = priority;
    }

    onInteractionOccurs(callback) {
        this.addEventListener(events.interaction, callback);
    }

    interactionOccurs() {
        this.emit(events.interaction);
    }

    requestRender() {
        this.emit(events.render.request);
    }

    /**
     * Test point hits zone
     * @param {InteractiveZoneEvent} [event]
     */
    // eslint-disable-next-line no-unused-vars
    test (event) {
        return false;
    }

    /**
     * Checks if zone is able to drag
     * @param {InteractiveZoneEvent} event
     * @returns {boolean} if zone is able to drag
     */
    // eslint-disable-next-line no-unused-vars
    shouldDrag(event) {
        return true;
    }

    /**
     *
     * @param {InteractiveZoneEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    onDragStart(event) {

    }
    /**
     *
     * @param {InteractiveZoneDragEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    onDrag(event) {
        this.interactionOccurs();
    }
    /**
     *
     * @param {InteractiveZoneDragEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    onDragEnd(event) {

    }

    /**
     *
     * @param {InteractiveZoneEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    onDoubleClick(event) {
        this.interactionOccurs();
    }

    /**
     *
     * @param {InteractiveZoneEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    onClick(event) {
        this.interactionOccurs();
    }

    /**
     *
     * @param {InteractiveZoneScrollEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    onScroll(event) {
        this.interactionOccurs();
    }

    /**
     *
     * @param {InteractiveZoneEvent} event
     */
    // eslint-disable-next-line no-unused-vars
    onHover(event) {

    }

    destroy() {
        this.container = undefined;
    }
}

const ZOOM_RATIO_STEP = 0.05;

class DefaultInteractiveZone extends InteractiveZone {
    /**
     *
     * @param {HeatmapViewport} viewport
     */
    constructor(viewport) {
        super();
        this.viewport = viewport;
    }

    test(event) {
        return event.fits();
    }

    onScroll(event) {
        super.onScroll(event);
        if (event.shift && this.viewport) {
            event.preventDefault();
            event.stopImmediatePropagation();
            this.viewport.zoom(
                {
                    ratio: 1.0 + event.deltaY * ZOOM_RATIO_STEP,
                    anchor: {column: event.column, row: event.row}
                },
                false
            );
        }
    }

    onDoubleClick(event) {
        super.onDoubleClick(event);
        this.viewport.zoomIn({
            column: event.column,
            row: event.row
        });
    }
}

export {DefaultInteractiveZone};
export default InteractiveZone;
