import {CachedTrackRenderer} from '../../core';
import PIXI from 'pixi.js';

const Math = window.Math;

export default class WIGRenderer extends CachedTrackRenderer{

    _hoveredItemContainer;

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

    static getBaseScale(value) {
        return value > 0 ? (Math.log(value) / Math.log(10)) : 0;
    }

    _changeWig(viewport, wig, coordinateSystem) {
        if (wig === null || wig === undefined || coordinateSystem === null || coordinateSystem === undefined)
            return;
        if (this.dataContainer.children.length > 0) {
            this.dataContainer.removeChildren(0, this.dataContainer.children.length);
        }
        this._hoveredItemContainer = new PIXI.Container();
        {
            const {block, line} = this._renderItems(wig.items.aboveBaseAxis, this._config.wig.color, this._config.wig.lineColor, viewport, wig, coordinateSystem);
            this.dataContainer.addChild(block);
            this.dataContainer.addChild(line);
        }
        {
            const {block, line} = this._renderItems(wig.items.belowBaseAxis, this._config.wig.color, this._config.wig.lineColor, viewport, wig, coordinateSystem);
            this.dataContainer.addChild(block);
            this.dataContainer.addChild(line);
        }
        {
            const {block, line} = this._renderItems(wig.thresholdItems.aboveBaseAxis, this._config.wig.thresholdColor, this._config.wig.lineThresholdColor, viewport, wig, coordinateSystem);
            this.dataContainer.addChild(block);
            this.dataContainer.addChild(line);
        }
        {
            const {block, line} = this._renderItems(wig.thresholdItems.belowBaseAxis, this._config.wig.thresholdColor, this._config.wig.lineThresholdColor, viewport, wig, coordinateSystem);
            this.dataContainer.addChild(block);
            this.dataContainer.addChild(line);
        }
        this.dataContainer.addChild(this._hoveredItemContainer);
    }

    hoverItem(item, viewport, wig, coordinateSystem) {
        if (!this._hoveredItemContainer) {
            return;
        }
        this._hoveredItemContainer.removeChildren();
        if (item) {
            const block = new PIXI.Graphics();
            const line = new PIXI.Graphics();

            const isDetailed = wig.isDetailed || viewport.factor > this._config.wig.detailedStyleStartingAtPixelsPerBP;

            block.beginFill(0x000000, viewport.factor > 2 ? 0.125 : 0.5);
            const lineThickness = 1;
            line.lineStyle(lineThickness, 0x000000, viewport.factor > 2 ? 0.5 : 1);

            const padding = isDetailed ? 0.5 : 0;
            const startX1 = Math.round(viewport.project.brushBP2pixel(item.startIndex - 0.5) + padding);
            let startX2 = Math.round(viewport.project.brushBP2pixel(item.endIndex + 0.5) - padding);
            if (startX1 === startX2) {
                startX2 ++;
            }
            block.moveTo(startX1, this._getYValue(wig.baseAxis, coordinateSystem));
            block.lineTo(startX1, this._getYValue(item.dataValue, coordinateSystem));
            block.lineTo(startX2, this._getYValue(item.dataValue, coordinateSystem));
            block.lineTo(startX2, this._getYValue(wig.baseAxis, coordinateSystem));
            block.lineTo(startX1, this._getYValue(wig.baseAxis, coordinateSystem));


            line.moveTo(startX1, this._getYValue(item.dataValue, coordinateSystem) - lineThickness / 2);
            line.lineTo(startX2, this._getYValue(item.dataValue, coordinateSystem) - lineThickness / 2);

            if (!isDetailed) {
                line.lineStyle(lineThickness, 0xf9f9f9, 1);
                line.moveTo(startX1 - lineThickness / 2, this._getYValue(wig.baseAxis, coordinateSystem) - lineThickness / 2);
                line.lineTo(startX1 - lineThickness / 2, 0);
                line.moveTo(startX2 + lineThickness / 2, this._getYValue(wig.baseAxis, coordinateSystem) - lineThickness / 2);
                line.lineTo(startX2 + lineThickness / 2, 0);
            }

            block.endFill();

            this._hoveredItemContainer.addChild(block);
            this._hoveredItemContainer.addChild(line);
        }
    }

    _renderItems(items, color, lineColor, viewport, wig, coordinateSystem) {
        if (coordinateSystem && coordinateSystem.isHeatMap) {
            return this._renderHeatMapItems(items, color, lineColor, viewport, wig, coordinateSystem);
        }
        const block = new PIXI.Graphics();
        const line = new PIXI.Graphics();

        const pixelsPerBp = viewport.factor;

        block.beginFill(color, 1);
        const lineThickness = 1;
        line.lineStyle(lineThickness, lineColor, 1);

        let count = 0;

        for (let i = 0; i < items.length; i++){
            const item = items[i];
            count++;
            if (item.points.length > 0) {
                const start = item.points[0];
                const padding = 0.5;
                if (item.points.length === 1 || (wig.isDetailed && pixelsPerBp >= this._config.wig.detailedStyleStartingAtPixelsPerBP)){
                    const startX1 = Math.round(viewport.project.brushBP2pixel(start.startIndex - 0.5) + padding);
                    let startX2 = Math.round(viewport.project.brushBP2pixel(start.endIndex + 0.5) - padding);

                    if (startX1 === startX2) {
                        startX2++; // bar should have minimum 1px width
                    }

                    block.moveTo(startX1, this._getYValue(wig.baseAxis, coordinateSystem));
                    block.lineTo(startX1, this._getYValue(start.dataValue, coordinateSystem));
                    block.lineTo(startX2, this._getYValue(start.dataValue, coordinateSystem));
                    block.lineTo(startX2, this._getYValue(wig.baseAxis, coordinateSystem));

                    line.moveTo(startX1, this._getYValue(start.dataValue, coordinateSystem) - lineThickness / 2);
                    line.lineTo(startX2, this._getYValue(start.dataValue, coordinateSystem) - lineThickness / 2);

                    let prevX = startX2;

                    for (let j = 1; j < item.points.length; j++){
                        count++;
                        const point = item.points[j];
                        let startX = Math.round(viewport.project.brushBP2pixel(point.startIndex - 0.5) + padding);
                        const endX = Math.round(viewport.project.brushBP2pixel(point.endIndex + 0.5) - padding);
                        if (startX === prevX) {
                            startX ++;
                        }
                        block.lineTo(startX, this._getYValue(wig.baseAxis, coordinateSystem));
                        block.lineTo(startX, this._getYValue(point.dataValue, coordinateSystem));
                        block.lineTo(endX, this._getYValue(point.dataValue, coordinateSystem));
                        block.lineTo(endX, this._getYValue(wig.baseAxis, coordinateSystem));

                        line.moveTo(startX, this._getYValue(point.dataValue, coordinateSystem) - lineThickness / 2);
                        line.lineTo(endX, this._getYValue(point.dataValue, coordinateSystem) - lineThickness / 2);

                        prevX = endX;
                    }

                    block.lineTo(startX1, this._getYValue(wig.baseAxis, coordinateSystem));
                }
                else {

                    const startX1 = Math.round(viewport.project.brushBP2pixel(start.startIndex - 0.5) + padding);
                    const startX2 = Math.round(viewport.project.brushBP2pixel(start.endIndex + 0.5) - padding);

                    block.moveTo(startX1, this._getYValue(wig.baseAxis, coordinateSystem));
                    block.lineTo(startX1, this._getYValue(start.dataValue, coordinateSystem));
                    block.lineTo(startX2, this._getYValue(start.dataValue, coordinateSystem));

                    line.moveTo(startX1 - lineThickness / 2, this._getYValue(wig.baseAxis, coordinateSystem));
                    line.lineTo(startX1 - lineThickness / 2, this._getYValue(start.dataValue, coordinateSystem) - lineThickness / 2);
                    line.lineTo(startX1, this._getYValue(start.dataValue, coordinateSystem) - lineThickness / 2);
                    line.lineTo(startX2, this._getYValue(start.dataValue, coordinateSystem) - lineThickness / 2);

                    for (let j = 1; j < item.points.length; j++){
                        count++;
                        const point = item.points[j];
                        const x1 = Math.round(viewport.project.brushBP2pixel(point.startIndex - 0.5) + padding);
                        const x2 = Math.round(viewport.project.brushBP2pixel(point.endIndex + 0.5) - padding);
                        block.lineTo(x1, this._getYValue(point.dataValue, coordinateSystem));
                        block.lineTo(x2, this._getYValue(point.dataValue, coordinateSystem));

                        line.lineTo(x1, this._getYValue(point.dataValue, coordinateSystem) - lineThickness / 2);
                        line.lineTo(x2, this._getYValue(point.dataValue, coordinateSystem) - lineThickness / 2);
                    }

                    const end = item.points[item.points.length - 1];
                    const endX = Math.round(viewport.project.brushBP2pixel(end.endIndex + 0.5) - padding);
                    block.lineTo(endX, this._getYValue(wig.baseAxis, coordinateSystem));
                    block.lineTo(startX1, this._getYValue(wig.baseAxis, coordinateSystem));

                    line.lineTo(endX - lineThickness / 2, this._getYValue(end.dataValue, coordinateSystem) - lineThickness / 2);
                    line.lineTo(endX - lineThickness / 2, this._getYValue(wig.baseAxis, coordinateSystem) - lineThickness / 2);
                }
            }
        }
        block.endFill();
        return {block, line};
    }

    _renderHeatMapItems(items, color, lineColor, viewport, wig, coordinateSystem) {
        const block = new PIXI.Graphics();
        const line = new PIXI.Graphics();
        for (let i = 0; i < items.length; i++){
            const item = items[i];
            for (let ii = 0; ii < item.points.length; ii++) {
                const point = item.points[ii];
                const percent = this._getPercentValue(point.dataValue, coordinateSystem);
                if (percent > 0) {
                    const x1 = Math.round(viewport.project.brushBP2pixel(point.startIndex - 0.5));
                    const x2 = Math.round(viewport.project.brushBP2pixel(point.endIndex + 0.5));
                    block.beginFill(color, percent);
                    block.drawRect(
                        Math.min(x1, x2),
                        0,
                        Math.abs(x2 - x1),
                        this.height
                    );
                    block.endFill();
                }
            }
        }
        return {block, line};
    }

    _getYValue(value, coordinateSystem) {
        if (coordinateSystem.isLogScale) {
            value = WIGRenderer.getBaseScale(value);
        }
        return Math.round(this.height - this.height * (value - coordinateSystem.minimum) / (coordinateSystem.maximum - coordinateSystem.minimum));
    }

    _getPercentValue(value, coordinateSystem) {
        return (value - coordinateSystem.minimum) / (coordinateSystem.maximum - coordinateSystem.minimum);
    }

    onMove(viewport, cursor, data) {
        if (data && data.items && data.thresholdItems) {
            return this._checkItems(viewport, cursor, data.items.aboveBaseAxis, {above: true, threshold: false}) ||
                this._checkItems(viewport, cursor, data.items.belowBaseAxis, {above: false, threshold: false}) ||
                this._checkItems(viewport, cursor, data.thresholdItems.aboveBaseAxis, {above: true, threshold: true}) ||
                this._checkItems(viewport, cursor, data.thresholdItems.belowBaseAxis, {above: false, threshold: true});
        }
        return null;
    }

    _checkItems(viewport, cursor, items, config){
        if (!items)
            return null;
        for (let i = 0; i < items.length; i++){
            const item = items[i];
            for (let j = 0; j < item.points.length; j++) {
                const point = item.points[j];
                if (cursor.x >= this.correctedXPosition(viewport.project.brushBP2pixel(point.startIndex - 0.5)) &&
                    cursor.x < this.correctedXPosition(viewport.project.brushBP2pixel(point.endIndex + 0.5))) {
                    return {...point, config};
                }
            }
        }
        return null;
    }
}
