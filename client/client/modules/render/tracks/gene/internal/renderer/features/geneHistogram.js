import * as PIXI from 'pixi.js';
import {Viewport} from '../../../../../core';
import {ColorProcessor} from '../../../../../utilities';

const Math = window.Math;

export default class GeneHistogram extends PIXI.Container {

    _config = null;

    constructor(config){
        super();
        this._config = config;
        this._graphics = new PIXI.Graphics();
        this._hoveredItemContainer = new PIXI.Container();
        this.addChild(this._graphics);
        this.addChild(this._hoveredItemContainer);
    }

    totalHeight;
    _graphics;
    _hoveredItemContainer;

    get config() { return this._config; }

    renderHistogram(viewport: Viewport, histogramData){
        this._graphics.clear();
        this._graphics.beginFill(this.config.histogram.fill, 1);
        let prevX = null;
        for (let i = histogramData.start; i < histogramData.end; i++){
            const item = histogramData.items[i];
            let x1 = viewport.project.brushBP2pixel(item.startIndex);
            let x2 = viewport.project.brushBP2pixel(item.endIndex);
            if (!prevX || Math.round(prevX) === Math.round(x1)) {
                x1++;
            }
            if (Math.round(x2 - x1) < 1) {
                x2 ++;
            }
            const height = (this.totalHeight - this.config.levels.margin) * item.value / histogramData.max;
            this._graphics
                .drawRect(Math.round(x1),
                    Math.round(this.totalHeight - height),
                    Math.round(x2 - x1),
                    Math.round(height));
            prevX = Math.round(x1) + Math.round(x2 - x1);
        }
        this._graphics.endFill();
    }

    clear() {
        this._graphics.clear();
        this._hoveredItemContainer.removeChildren();
    }

    checkPosition(position, histogramData, viewport) {
        return histogramData.items.filter(function(item) {
            const x1 = viewport.project.brushBP2pixel(item.startIndex);
            const x2 = viewport.project.brushBP2pixel(item.endIndex);
            return (position.x >= x1 && position.x <= x2);
        });
    }

    hoverItem(hoveredItem, viewport, histogramData) {
        this._hoveredItemContainer.removeChildren();
        if (hoveredItem) {
            const graphics = new PIXI.Graphics();
            graphics.clear();
            graphics.beginFill(ColorProcessor.darkenColor(this.config.histogram.fill, 0.2), 1);
            const item = hoveredItem[0];
            let x1 = viewport.project.brushBP2pixel(item.startIndex);
            let x2 = viewport.project.brushBP2pixel(item.endIndex);
            if (Math.round(x2 - x1) < 1) {
                x2 ++;
            }
            const height = (this.totalHeight - this.config.levels.margin) * item.value / histogramData.max;
            graphics
                .drawRect(Math.round(x1),
                    Math.round(this.totalHeight - height),
                    Math.round(x2 - x1),
                    Math.round(height));
            graphics.endFill();
            this._hoveredItemContainer.addChild(graphics);
        }
        return true;
    }
}
