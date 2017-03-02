import {CachedTrackRenderer} from '../../core';
import PIXI from 'pixi.js';

const Math = window.Math;

export default class WIGRenderer extends CachedTrackRenderer{

    _height = null;

    constructor(config){
        super();
        this._config = config;
        this._height = config.height;
    }

    get height() { return this._height; }
    set height(value) { this._height = value; }

    rebuildContainer(viewport, cache){
        super.rebuildContainer(viewport, cache);
        this._changeWig(viewport, cache.data, cache.coordinateSystem);
    }

    _changeWig(viewport, wig, coordinateSystem){
        if (wig === null || wig === undefined || coordinateSystem === null || coordinateSystem === undefined)
            return;
        if (this.dataContainer.children.length > 0) {
            this.dataContainer.removeChildren(0, this.dataContainer.children.length);
        }
        this.dataContainer.addChild(this._renderItems(wig.items.aboveBaseAxis, this._config.wig.color,
            viewport, wig, coordinateSystem));
        this.dataContainer.addChild(this._renderItems(wig.items.belowBaseAxis, this._config.wig.color,
            viewport, wig, coordinateSystem));
        this.dataContainer.addChild(this._renderItems(wig.thresholdItems.aboveBaseAxis, this._config.wig.thresholdColor,
            viewport, wig, coordinateSystem));
        this.dataContainer.addChild(this._renderItems(wig.thresholdItems.belowBaseAxis, this._config.wig.thresholdColor,
            viewport, wig, coordinateSystem));
    }

    _renderItems(items, color, viewport, wig, coordinateSystem){
        const block = new PIXI.Graphics();

        const pixelsPerBp = viewport.factor;

        block.beginFill(color, 1);

        for (let i = 0; i < items.length; i++){
            const item = items[i];
            if (item.points.length > 0) {
                const start = item.points[0];
                if (item.points.length === 1 || (wig.isDetailed && pixelsPerBp >= this._config.wig.detailedStyleStartingAtPixelsPerBP)){
                    const pixelsOffset = 0.5;
                    const padding = Math.max(0.5, pixelsPerBp / 2.0 - pixelsOffset);
                    block.moveTo(Math.round(this.correctedXPosition(start.xStart) - padding),
                        this._getYValue(wig.baseAxis, coordinateSystem));

                    block.lineTo(Math.round(this.correctedXPosition(start.xStart) - padding),
                        this._getYValue(start.dataValue, coordinateSystem));
                    block.lineTo(Math.round(this.correctedXPosition(start.xEnd) + padding),
                        this._getYValue(start.dataValue, coordinateSystem));
                    block.lineTo(Math.round(this.correctedXPosition(start.xEnd) + padding),
                        this._getYValue(wig.baseAxis, coordinateSystem));

                    for (let j = 1; j < item.points.length; j++){
                        const point = item.points[j];
                        block.lineTo(Math.round(this.correctedXPosition(point.xStart) - padding),
                            this._getYValue(wig.baseAxis, coordinateSystem));
                        block.lineTo(Math.round(this.correctedXPosition(point.xStart) - padding),
                            this._getYValue(point.dataValue, coordinateSystem));
                        block.lineTo(Math.round(this.correctedXPosition(point.xEnd) + padding),
                            this._getYValue(point.dataValue, coordinateSystem));
                        block.lineTo(Math.round(this.correctedXPosition(point.xEnd) + padding),
                            this._getYValue(wig.baseAxis, coordinateSystem));
                    }

                    block.lineTo(Math.round(this.correctedXPosition(start.xStart) - padding),
                        this._getYValue(wig.baseAxis, coordinateSystem));
                }
                else {
                    block.moveTo(this.correctedXPosition(start.xStart),
                        this._getYValue(wig.baseAxis, coordinateSystem));
                    block.lineTo(this.correctedXPosition(start.xStart),
                        this._getYValue(start.dataValue, coordinateSystem));
                    block.lineTo(this.correctedXPosition(start.xEnd),
                        this._getYValue(start.dataValue, coordinateSystem));

                    for (let j = 1; j < item.points.length; j++){
                        const point = item.points[j];
                        block.lineTo(this.correctedXPosition(point.xStart),
                            this._getYValue(point.dataValue, coordinateSystem));
                        block.lineTo(this.correctedXPosition(point.xEnd),
                            this._getYValue(point.dataValue, coordinateSystem));
                    }

                    const end = item.points[item.points.length - 1];
                    block.lineTo(this.correctedXPosition(end.xEnd),
                        this._getYValue(wig.baseAxis, coordinateSystem));
                    block.lineTo(this.correctedXPosition(start.xStart),
                        this._getYValue(wig.baseAxis, coordinateSystem));
                }
            }
        }
        block.endFill();
        return block;
    }

    _getYValue(bpValue, coordinateSystem){
        return this.height - this.height * (bpValue - coordinateSystem.minimum) / (coordinateSystem.maximum - coordinateSystem.minimum);
    }

    onMove(viewport, cursor, data) {
        if (data && data.items && data.thresholdItems) {
            return this._checkItems(viewport, cursor, data.items.aboveBaseAxis) ||
                this._checkItems(viewport, cursor, data.items.belowBaseAxis) ||
                this._checkItems(viewport, cursor, data.thresholdItems.aboveBaseAxis) ||
                this._checkItems(viewport, cursor, data.thresholdItems.belowBaseAxis);
        }
        return null;
    }

    _checkItems(viewport, cursor, items){
        if (!items)
            return null;
        const pixelsPerBp = viewport.factor;
        const padding = pixelsPerBp / 2.0;
        for (let i = 0; i < items.length; i++){
            const item = items[i];
            for (let j = 0; j < item.points.length; j++) {
                const point = item.points[j];
                if (cursor.x >= this.correctedXPosition(point.xStart) - padding && cursor.x < this.correctedXPosition(point.xEnd) + padding) {
                    return point.dataItem;
                }
            }
        }
        return null;
    }
}
