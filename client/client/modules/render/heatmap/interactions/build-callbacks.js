import {
    InteractiveZoneClickEvent,
    InteractiveZoneDragEvent,
    InteractiveZoneEvent,
    InteractiveZoneScrollEvent
} from './events';

const DOUBLE_CLICK_TIMEOUT = 100;
const DISABLE_CLICK_TIMEOUT = 100;

const functionMock = () => undefined;
const functionMockArray = () => ([]);

/**
 * @typedef {Object} HeatmapInteractionCallbacks
 * @property {function(e)} doubleClick
 * @property {function(e)} mouseMove
 * @property {function(e)} mouseOut
 * @property {function(e)} mouseUp
 * @property {function(e)} mouseOver
 * @property {function(e)} mouseDown
 * @property {function(e)} click
 * @property {function(e)} mouseWheel
 */

/**
 * Builds mouse event handlers
 * @param {HeatmapViewport} viewport
 * @param {function(e: InteractiveZoneEvent, zones: InteractiveZone[])} [onHover]
 * @param {function(e: InteractiveZoneEvent): InteractiveZone[]} checkInteractiveZone
 * @return {HeatmapInteractionCallbacks}
 */
export default function buildCallbacks(
    viewport,
    onHover = functionMock,
    checkInteractiveZone = functionMockArray
) {
    let mouseDownEvent = undefined;
    let moved = undefined;
    let clickHandler = undefined;
    let disableClickHandler = undefined;
    let clickDisabled = false;
    let over = false;
    /**
     *
     * @type {InteractiveZone[]}
     */
    let draggingZones = [];
    const clearClickHandler = () => {
        if (clickHandler) {
            clearTimeout(clickHandler);
            clickHandler = undefined;
        }
    };
    const clearDisableClickHandler = () => {
        if (disableClickHandler) {
            clearTimeout(disableClickHandler);
            disableClickHandler = undefined;
        }
        clickDisabled = false;
    };
    const mouseOver = () => {
        over = true;
    };
    const mouseOut = () => {
        over = false;
        onHover();
    };
    const mouseDown = (e) => {
        onHover();
        mouseDownEvent = new InteractiveZoneEvent(e, viewport);
        draggingZones = checkInteractiveZone(mouseDownEvent)
            .map(zone => {
                if (mouseDownEvent.immediatePropagationStopped) {
                    return undefined;
                }
                if (zone.shouldDrag(mouseDownEvent)) {
                    zone.onDragStart(mouseDownEvent);
                    return zone;
                }
                return undefined;
            })
            .filter(Boolean);
    };
    const mouseMove = (e) => {
        const info = new InteractiveZoneEvent(e, viewport);
        if (mouseDownEvent) {
            onHover();
            const dragEvent = new InteractiveZoneDragEvent(e, viewport, mouseDownEvent);
            moved = true;
            e.stopPropagation();
            e.preventDefault();
            draggingZones.forEach(zone => {
                if (!dragEvent.immediatePropagationStopped) {
                    zone.onDrag(dragEvent);
                }
            });
        } else if (over) {
            onHover(info, checkInteractiveZone(info));
        } else {
            onHover();
        }
    };
    const mouseUp = (e) => {
        if (mouseDownEvent && moved) {
            mouseMove(e);
            const dragEndEvent = new InteractiveZoneDragEvent(e, viewport, mouseDownEvent);
            e.stopImmediatePropagation();
            clearDisableClickHandler();
            clickDisabled = true;
            disableClickHandler = setTimeout(clearDisableClickHandler, DISABLE_CLICK_TIMEOUT);
            if (draggingZones.length > 0) {
                draggingZones.forEach(zone => {
                    if (!dragEndEvent.immediatePropagationStopped) {
                        zone.onDragEnd(dragEndEvent);
                    }
                });
            }
        }
        draggingZones = [];
        moved = false;
        mouseDownEvent = undefined;
    };
    const handleClick = (clickEvent) => {
        clearClickHandler();
        if (!clickEvent) {
            return;
        }
        const zones = checkInteractiveZone(clickEvent);
        if (clickEvent.doubleClick) {
            zones.forEach(zone => {
                if (!clickEvent.immediatePropagationStopped) {
                    zone.onDoubleClick(clickEvent);
                }
            });
        } else {
            zones.forEach(zone => {
                if (!clickEvent.immediatePropagationStopped) {
                    zone.onClick(clickEvent);
                }
            });
        }
    };
    const click = (e) => {
        if (clickDisabled) {
            clearDisableClickHandler();
            clickDisabled = false;
            return;
        }
        const clickInfo = new InteractiveZoneClickEvent(e, viewport);
        clearClickHandler();
        clickHandler = setTimeout(() => handleClick(clickInfo), DOUBLE_CLICK_TIMEOUT);
    };
    const doubleClick = (e) => {
        clearClickHandler();
        const clickInfo = new InteractiveZoneClickEvent(e, viewport, true);
        handleClick(clickInfo);
    };
    const mouseWheel = (e) => {
        if (mouseDownEvent && moved) {
            return;
        }
        const event = new InteractiveZoneScrollEvent(e, viewport);
        const zones = checkInteractiveZone(event);
        zones.forEach(zone => {
            if (!event.immediatePropagationStopped) {
                zone.onScroll(event);
            }
        });
        if (event.defaultIsPrevented) {
            e.preventDefault();
        }
        onHover();
    };
    return {
        mouseOver,
        mouseOut,
        mouseDown,
        mouseMove,
        mouseUp,
        click,
        doubleClick,
        mouseWheel
    };
}
