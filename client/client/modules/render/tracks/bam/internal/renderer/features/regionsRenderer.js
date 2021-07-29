import * as PIXI from 'pixi.js';
import {ColorProcessor} from '../../../../../utilities';

const Math = window.Math;

export class RegionsRenderer{

    _height = null;
    container = new PIXI.Container();

    constructor(config) {
        this._config = config;
        this._height = config.height;
    }

    get height() { return this._height; }
    set height(value) { this._height = value; }

    render(viewport, items, selectedItem) {
        if (items === null || items === undefined || items.length === 0)
            return;
        this.container.removeChildren();
        const line = new PIXI.Graphics();

        let max = 1;

        for (let i = 0; i < items.length; i++) {
            const item = items[i];
            if (item.value === 0 ||
                item.startIndex > viewport.brush.end + viewport.brushSize ||
                item.endIndex < viewport.brush.start - viewport.brushSize) {
                continue;
            }
            if (max < item.value) {
                max = item.value;
            }
        }

        const renderLineFn = (start, boundaries) => {
            const {x1, x2} = boundaries;
            const pointA = {x: start, y: this.height};
            const pointB = {x: start + this.height, y: 0};
            if (pointA.x < x1) {
                pointA.x = x1;
                pointA.y = pointB.x - x1;
            }
            if (pointB.x > x2) {
                pointB.y = pointB.x - x2;
                pointB.x = x2;
            }
            line.moveTo(pointA.x, pointA.y);
            line.lineTo(pointB.x, pointB.y);
        };

        const renderFn = (item) => {
            const delta = (this._config.lines.alphaMax - this._config.lines.alpha) / 5;
            const alpha = Math.min(this._config.lines.alpha + delta * (max - item.value), this._config.lines.alphaMax);
            const hovered = selectedItem && selectedItem.startIndex === item.startIndex && selectedItem.endIndex === item.endIndex;
            line.lineStyle(this._config.lines.thickness, hovered ? ColorProcessor.darkenColor(this._config.lines.fill) : this._config.lines.fill, alpha);
            const startPx = Math.max(- viewport.canvasSize, viewport.project.brushBP2pixel(item.startIndex));
            const endPx = Math.min(2 * viewport.canvasSize, viewport.project.brushBP2pixel(item.endIndex));
            for (let i = startPx - this.height; i < endPx; i += this._config.lines.step) {
                renderLineFn(i, {x1: startPx, x2: endPx});
            }
        };

        for (let i = 0; i < items.length; i++) {
            const item = items[i];
            if (item.value === 0 ||
                item.startIndex > viewport.brush.end + viewport.brushSize ||
                item.endIndex < viewport.brush.start - viewport.brushSize) {
                continue;
            }
            renderFn(item);
        }

        this.container.addChild(line);
    }


}
