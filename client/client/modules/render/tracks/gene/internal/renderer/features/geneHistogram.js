import PIXI from 'pixi.js';
import {Viewport} from '../../../../../core';

export default class GeneHistogram extends PIXI.Graphics{

    _config = null;

    constructor(config){
        super();
        this._config = config;
    }

    get config() { return this._config; }

    renderHistogram(viewport: Viewport, histogramData){
        this.clear();
        this.beginFill(this.config.histogram.fill, 1);
        for (let i = histogramData.start; i < histogramData.end; i++){
            const item = histogramData.items[i];
            let x = viewport.project.brushBP2pixel(item.startIndex);
            const y = this.config.levels.margin;
            let width = viewport.convert.brushBP2pixel(item.endIndex - item.startIndex);
            const separateBarsThreshold = 3;
            const separateBarsOffset = 0.5;
            if (width > separateBarsThreshold){
                width -= (2 * separateBarsOffset);
                x += separateBarsOffset;
            }
            const height = this.config.histogram.height * item.value / histogramData.max;
            this
                .drawRect(x, y + this.config.histogram.height - height, width, height);
        }
        this.endFill();
    }

    checkPosition(position, histogramData, viewport, yMargin, configHeight) {
        return histogramData.items.filter(function(item) {
            const x = viewport.project.brushBP2pixel(item.startIndex);
            const width = viewport.convert.brushBP2pixel(item.endIndex - item.startIndex);
            const height = configHeight * item.value / histogramData.max;
            const y = yMargin + configHeight - height;
            return (position.x >= x && position.x <= x + width
                    && position.y >= y && position.y <= y + height);
        });
    }
}
