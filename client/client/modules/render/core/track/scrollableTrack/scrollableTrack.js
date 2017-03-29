import {DRAG_STAGE_DRAGGING, DRAG_STAGE_FINISHED, DRAG_STAGE_STARTED, InteractiveTrack} from '../interactiveTrack';

export default class ScrollableTrack extends InteractiveTrack {

    _isScrollIndicatorDragging = false;
    _scrollIndicatorDragStartPosition = null;

    constructor(opts) {
        super(opts);
    }

    isScrollable() {
        return false;
    }

    scrollIndicatorBoundaries() {
        return null;
    }

    getScrollPosition() {
        const boundaries = this.scrollIndicatorBoundaries();
        if (boundaries) {
            return boundaries.y;
        }
        return 0;
    }

    setScrollPosition() {
        // should be overridden at derived classes
    }

    onDrag({currentGlobal, delta, stage, startGlobal, startLocal}) {
        if (this.isScrollable()) {
            let wasScroll = false;
            if (stage) {
                switch (stage) {
                    case DRAG_STAGE_STARTED: {
                        this._isScrollIndicatorDragging = this._positionFitsScrollIndicatorBoundaries(startLocal);
                        this._scrollIndicatorDragStartPosition = this.getScrollPosition();
                    }
                        break;
                    case DRAG_STAGE_DRAGGING: {
                        if (this._isScrollIndicatorDragging) {
                            this.setScrollPosition(this._scrollIndicatorDragStartPosition + currentGlobal.y - startGlobal.y);
                            this.updateScene();
                        }
                    }
                        break;
                    case DRAG_STAGE_FINISHED: {
                        if (this._isScrollIndicatorDragging) {
                            this.setScrollPosition(this._scrollIndicatorDragStartPosition + currentGlobal.y - startGlobal.y);
                            this.updateScene();
                        }
                        this._isScrollIndicatorDragging = false;
                        wasScroll = true;
                    }
                        break;
                }
            }
            if (!this._isScrollIndicatorDragging && !wasScroll && delta) {
                super.onDrag({delta, stage, startLocal});
            } else if (wasScroll) {
                super.onDrag({delta: 0, stage});
            }
            return;
        }
        super.onDrag({delta, stage, startLocal});
    }

    onHover({x, y}) {
        if (super.onHover({x, y})) {
            if (this._positionFitsScrollIndicatorBoundaries({x, y})) {
                if (this.hoverVerticalScroll()) {
                    this.updateScene();
                }
                this.tooltip.hide();
                return false;
            }
            else {
                if (this.unhoverVerticalScroll()) {
                    this.updateScene();
                }
            }
        }
        return true;
    }

    hoverVerticalScroll() {
        return true;
    }

    unhoverVerticalScroll() {
        return true;
    }

    _positionFitsScrollIndicatorBoundaries(position) {
        const boundaries = this.scrollIndicatorBoundaries();
        return boundaries && position.x >= boundaries.x && position.x <= boundaries.x + boundaries.width &&
            position.y >= boundaries.y && position.y <= boundaries.y + boundaries.height;
    }

}