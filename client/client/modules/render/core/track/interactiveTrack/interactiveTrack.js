import ModifierKeysState from '../../../../../app/shared/hotkeys';
import {Track} from '../track';
import angular from 'angular';

const autoincrement = () => autoincrement.value ? autoincrement.value += 1 : autoincrement.value = 1;

const DOUBLE_CLICK_TIMEOUT = 200;
const Math = window.Math;

export const DRAG_STAGE_STARTED = 'started';
export const DRAG_STAGE_DRAGGING = 'dragging';
export const DRAG_STAGE_FINISHED = 'finished';

export default class InteractiveTrack extends Track {

    _trackSystemName = `${this.constructor.name}_${autoincrement()}`;

    _touched = false;
    _dragged = false;
    _lastPosition = null;
    _startLocalPosition = null;
    _startGlobalPosition = null;
    _currentPosition = null;
    _isDoubleClick = false;

    static lockMoveEvent = false;

    constructor(opts) {
        super(opts);
        this.initializeEventListeners();
    }

    destructor() {
        if (this.disableEventListeners) {
            this.disableEventListeners();
        }
        super.destructor();
    }

    _onTouchBegan(event){
        InteractiveTrack.lockMoveEvent = true;
        this._touched = true;
        this._dragged = false;
        this._startGlobalPosition = this._lastPosition = {x: event.screenX, y: event.screenY};
        this._startLocalPosition = {x: event.offsetX, y: event.offsetY};

        this.onDrag({
            currentGlobal: {x: event.screenX, y: event.screenY},
            currentLocal: {x: event.offsetX, y: event.offsetY},
            stage: DRAG_STAGE_STARTED,
            startGlobal: this._startGlobalPosition,
            startLocal: this._startLocalPosition
        });
    }

    _onTouchMoved(event){
        this._currentPosition = {x: event.offsetX, y: event.offsetY};
        let delta = null;
        if (this._lastPosition) {
            delta = {
                x: event.screenX - this._lastPosition.x,
                y: event.screenY - this._lastPosition.y,
                isZero: function() { return this.x === 0 && this.y === 0; }
            };
        }

        if(delta && !delta.isZero()) {
            this._dragged = this._touched;
        }

        if (this._dragged && this._touched) {
            event.preventDefault();
            this.onDrag({
                currentGlobal: {x: event.screenX, y: event.screenY},
                currentLocal: {x: event.offsetX, y: event.offsetY},
                delta,
                stage: delta ? DRAG_STAGE_DRAGGING : DRAG_STAGE_STARTED,
                startGlobal: this._startGlobalPosition,
                startLocal: this._startLocalPosition
            });
        } else if (event.target === angular.element(this.domElement).find('canvas')[0] && !InteractiveTrack.lockMoveEvent) {
            this.onHover({x: event.offsetX, y: event.offsetY});
        } else {
            this.onMouseOut();
        }
        this._lastPosition = {x: event.screenX, y: event.screenY};
    }

    _onTouchEnded(event){
        InteractiveTrack.lockMoveEvent = false;
        let delta = null;
        if (this._lastPosition) {
            delta = {
                x: event.screenX - this._lastPosition.x,
                y: event.screenY - this._lastPosition.y
            };
        }
        if (this._dragged) {
            this.onDrag({
                currentGlobal: {x: event.screenX, y: event.screenY},
                currentLocal: {x: event.offsetX, y: event.offsetY},
                delta,
                stage: DRAG_STAGE_FINISHED,
                startGlobal: this._startGlobalPosition,
                startLocal: this._startLocalPosition
            }, true);
        }
        this._touched = false;
        this._lastPosition = null;
    }

    _onMouseOver() {
        window.jQuery(document.body)
            .on(`mousewheel.${this._trackSystemName}`, ::this._onMouseWheel);
        const self = this;
        this._removeWheelEventListener = function() {
            window.jQuery(document.body)
                .off(`mousewheel.${self._trackSystemName}`);
        };
        this.onMouseOver();
    }

    _onMouseOut() {
        if (this._removeWheelEventListener) {
            this._removeWheelEventListener();
            this._removeWheelEventListener = null;
        }
        this.onMouseOut();
    }

    _onClick(event) {
        const x = event.offsetX;
        const y = event.offsetY;
        if (!this._isDoubleClick && !this._dragged) {
            const self = this;
            const handler = function () {
                if (!self._isDoubleClick)
                    self.onClick({x, y});
            };
            setTimeout(handler, DOUBLE_CLICK_TIMEOUT);
        }
    }

    _onDoubleClick(event) {
        this.tooltip.hide();
        this._isDoubleClick = true;
        this.onDoubleClick({x: event.offsetX, y: event.offsetY});
        const self = this;
        const clearFlag = () => {
            self._isDoubleClick = false;
        };
        setTimeout(clearFlag, DOUBLE_CLICK_TIMEOUT);
    }

    _onMouseWheel(event) {
        this.tooltip.hide();
        if (ModifierKeysState.shift) {
            const canvasOffsetX = this.domElement.getBoundingClientRect().left;
            const delta = - Math.sign(event.deltaY || event.deltaX);
            const zoomFactor = 10;
            const newBrushSize = this.viewport.brushSize * (1 + delta / zoomFactor);
            const position = this.viewport.project.pixel2brushBP(event.offsetX - canvasOffsetX);
            const deltaFromStart = (event.offsetX - canvasOffsetX) / this.viewport.canvasSize * newBrushSize;
            let start = Math.max(1, position - deltaFromStart);
            const end = Math.min(this.viewport.chromosomeSize, start + newBrushSize);
            start = Math.max(1, end - newBrushSize);
            this.moveBrush({end, start});
            event.preventDefault();
            event.stopImmediatePropagation();
            this.onHover({x: event.offsetX, y: event.offsetY});
            return false;
        } else if (this.canScroll(event.deltaY)) {
            const deltaFactor = 20;
            this.onScroll({delta: event.deltaY * deltaFactor});
            this.requestRenderRefresh();
            event.preventDefault();
            event.stopImmediatePropagation();
            this.onHover({x: event.offsetX, y: event.offsetY});
            return false;
        }
        return true;
    }

    onMouseOut() {
        this.tooltip.hide();
    }

    onMouseOver() {
    }

    onDrag({delta, stage}) {
        if (delta) {
            let shouldRefreshRender = false;
            if (this.canScroll(delta.y)) {
                this.onScroll({delta: delta.y});
                shouldRefreshRender = true;
            }
            if (this.viewport.canTransform) {
                this.viewport.transform({delta: -this.viewport.convert.pixel2brushBP(delta.x), finish: stage === DRAG_STAGE_FINISHED});
                shouldRefreshRender = false;
            }
            if (shouldRefreshRender) {
                this.requestRenderRefresh();
            }
        } else if (stage === DRAG_STAGE_FINISHED) {
            this.viewport.transform({delta: 0, finish: true});
        }
        this.tooltip.hide();
    }

    onHover() {
        return true;
    }

    onClick() {
        this.tooltip.hide();
    }

    onScroll() {
        this.tooltip.hide();
    }

    onDoubleClick({x}) {
        this.tooltip.hide();
        const xOffset = 0.5;
        const localEventPoint = {
            x: (this.viewport.project.pixel2brushBP(x) + xOffset) >> 0
        };
        const zoomInFactor = 0.75;
        const newBrushSize = this.viewport.brushSize * zoomInFactor;
        this.viewport.transform({
            end: localEventPoint.x + newBrushSize / 2,
            start: localEventPoint.x - newBrushSize / 2
        });
    }

    initializeEventListeners() {
        const _onTouchBegan = ::this._onTouchBegan;
        const _onTouchMoved = ::this._onTouchMoved;
        const _onTouchEnded = ::this._onTouchEnded;
        const _onMouseOver = ::this._onMouseOver;
        const _onMouseOut = ::this._onMouseOut;
        const _onClick = ::this._onClick;
        const _onDoubleClick = ::this._onDoubleClick;
        window.jQuery(this.domElement)
            .on('mouseenter', _onMouseOver)
            .on('mouseleave', _onMouseOut)
            .on('mousedown', _onTouchBegan)
            .on('touchstart', _onTouchBegan)
            .on('click', _onClick)
            .on('dblclick', _onDoubleClick);
        window.jQuery(window)
            .on('mousemove', _onTouchMoved)
            .on('touchmove', _onTouchMoved)
            .on('mouseup', _onTouchEnded)
            .on('mouseupoutside', _onTouchEnded)
            .on('touchend', _onTouchEnded)
            .on('touchendoutside', _onTouchEnded);
        this.disableEventListeners = function() {
            if (this._removeWheelEventListener) {
                this._removeWheelEventListener();
                this._removeWheelEventListener = null;
            }
            window.jQuery(this.domElement)
                .off('mouseenter', _onMouseOver)
                .off('mouseleave', _onMouseOut)
                .off('mousedown', _onTouchBegan)
                .off('touchstart', _onTouchBegan)
                .off('click', _onClick)
                .off('dblclick', _onDoubleClick);
            window.jQuery(window)
                .off('mousemove', _onTouchMoved)
                .off('touchmove', _onTouchMoved)
                .off('mouseup', _onTouchEnded)
                .off('mouseupoutside', _onTouchEnded)
                .off('touchend', _onTouchEnded)
                .off('touchendoutside', _onTouchEnded);
        };
    }

    canScroll() {
        return false;
    }
}
