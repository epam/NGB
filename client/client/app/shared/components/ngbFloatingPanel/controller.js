import {
    getInitialPlacement,
} from './placement';
const WINDOW_PADDING = 5;

class NgbFloatingPanel {
    static get UID() {
        return 'ngbFloatingPanel';
    }

    constructor($scope, $element) {
        this.$scope = $scope;
        this.onMouseDownCallback = this.onMouseDown.bind(this);
        this.onMouseMoveCallback = this.onMouseMove.bind(this);
        this.onMouseUpCallback = this.onMouseUp.bind(this);
        this.panel = $element[0];
        this.titleContainer = $element.find('.ngb-floating-panel-title')[0];
        this.dragging = false;
    }

    get dragging() {
        return this._dragging;
    }

    set dragging(dragging) {
        if (this._dragging !== dragging) {
            this._dragging = dragging;
            if (this.panel) {
                this.panel.style.cursor = this._dragging ? 'move' : '';
            }
        }
    }

    get closable() {
        return typeof this.onClose === 'function';
    }

    $onInit() {
        if (this.titleContainer) {
            this.titleContainer.addEventListener('mousedown', this.onMouseDownCallback);
            this.titleContainer.addEventListener('mousemove', this.onMouseMoveCallback);
            this.titleContainer.addEventListener('mouseup', this.onMouseUpCallback);
            window.addEventListener('mousemove', this.onMouseMoveCallback);
            window.addEventListener('mouseup', this.onMouseUpCallback);
        }
        this.setPosition(getInitialPlacement(this.placement));

        let w, h;
        const checkPosition = () => {
            const {
                clientWidth = 0,
                clientHeight = 0,
            } = this.panel || {};
            if (w !== clientWidth || h !== clientHeight) {
                w = clientWidth;
                h = clientHeight;
                this.setPosition(this._position);
            }
            this.checkPositionRAF = requestAnimationFrame(checkPosition);
        };
        checkPosition();
    }

    $onDestroy() {
        cancelAnimationFrame(this.checkPositionRAF);
        if (this.titleContainer) {
            this.titleContainer.removeEventListener('mousedown', this.onMouseDownCallback);
            this.titleContainer.removeEventListener('mousemove', this.onMouseMoveCallback);
            this.titleContainer.removeEventListener('mouseup', this.onMouseUpCallback);
        }
        window.removeEventListener('mousemove', this.onMouseMoveCallback);
        window.removeEventListener('mouseup', this.onMouseUpCallback);
    }

    setPosition(position) {
        this._position = this.correctPosition(position);
        if (this.panel) {
            this.panel.style.left = `${this._position.x}px`;
            this.panel.style.top = `${this._position.y}px`;
        }
    }

    correctPosition(position) {
        const {
            x = 0,
            y = 0,
        } = position || {};
        if (this.panel) {
            const {
                clientWidth,
                clientHeight
            } = this.panel;
            const minX = WINDOW_PADDING;
            const minY = WINDOW_PADDING;
            const maxX = window.innerWidth - clientWidth - WINDOW_PADDING;
            const maxY = window.innerHeight - clientHeight - WINDOW_PADDING;
            return {
                x: Math.max(minX, Math.min(maxX, x)),
                y: Math.max(minY, Math.min(maxY, y)),
            };
        }
        return {
            x,
            y,
        };
    }

    onMouseDown(event) {
        if (event.button === 0) {
            this.dragging = {
                x: event.pageX,
                y: event.pageY,
                originalX: this._position.x,
                originalY: this._position.y,
            };
            event.stopPropagation();
            event.preventDefault();
        }
    }

    onMouseMove(event) {
        if (this.dragging) {
            this.setPosition({
                x: this.dragging.originalX + (event.pageX - this.dragging.x),
                y: this.dragging.originalY + (event.pageY - this.dragging.y),
            });
            event.stopPropagation();
            event.preventDefault();
        }
    }

    onMouseUp(event) {
        if (this.dragging) {
            this.onMouseMove(event);
            event.stopPropagation();
            event.preventDefault();
        }
        this.dragging = false;
    }

    closePanel(event) {
        if (typeof this.onClose === 'function') {
            event.stopPropagation();
            event.preventDefault();
            this.onClose(event);
        }
    }
}

export default NgbFloatingPanel;
