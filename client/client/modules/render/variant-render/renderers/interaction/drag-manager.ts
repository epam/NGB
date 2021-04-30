import PIXI from 'pixi.js';

export default class DragManager{

    _config = null;
    _container: PIXI.Container;
    _drag = new Map();
    _draggableElements = new Map();

    get config() { return this._config; }
    get container(): PIXI.Container { return this._container; }

    constructor(config, container){
        this._config = config;
        this._container = container;
    }

    addDraggableZone(boundaries, callback) {
        const graphics = new PIXI.Graphics();
        graphics
            .beginFill(0xFFFFFF, 0)
            .drawRect(boundaries.x, boundaries.y, boundaries.width, boundaries.height)
            .endFill();
        graphics.interactive = true;
        this._registerDraggableGraphics(graphics, callback);
        this.container.addChild(graphics);
    }

    _registerDraggableGraphics(graphics, drag){
        this._draggableElements.set(graphics, drag);
        graphics
            .on('mousedown', this.onDragStart.bind(this))
            .on('touchstart', this.onDragStart.bind(this))
            // events for drag end
            .on('mouseup', this.onDragEnd.bind(this))
            .on('mouseupoutside', this.onDragEnd.bind(this))
            .on('touchend', this.onDragEnd.bind(this))
            .on('touchendoutside', this.onDragEnd.bind(this))
            // events for drag move
            .on('mousemove', this.onDragMove.bind(this))
            .on('touchmove', this.onDragMove.bind(this));
    }

    onDragStart(event) {
        this._drag.set(event.target, {
            point: event.data.global.clone()
        });
    }

    onDragEnd(event) {
        const dragData = this._drag.get(event.target);
        if (dragData) {
            const dragCallback = this._draggableElements.get(event.target);
            if (dragCallback) {
                const oldPoint = this.container.toLocal(dragData.point).x;
                const newPoint = this.container.toLocal(event.data.global).x;
                const dx = newPoint - oldPoint;
                dragCallback(dx);
            }
        }
        this._drag.set(event.target, null);
    }

    onDragMove(event) {
        const dragData = this._drag.get(event.target);
        if (dragData) {
            const dragCallback = this._draggableElements.get(event.target);
            if (dragCallback) {
                const oldPoint = this.container.toLocal(dragData.point).x;
                const newPoint = this.container.toLocal(event.data.global).x;
                const dx = newPoint - oldPoint;
                dragCallback(dx);
                dragData.point = event.data.global.clone();
            }
        }
    }
}
