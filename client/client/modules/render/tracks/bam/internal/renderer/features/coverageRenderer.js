import WIGRenderer from '../../../../wig/wigRenderer';

const Math = window.Math;

export class CoverageRenderer extends WIGRenderer {

    _bamConfig;

    constructor(config, bamConfig) {
        super(config);
        this._bamConfig = bamConfig;
    }

    _renderItems(items, color, lineColor, viewport, cache, coordinateSystem) {
        const {block, line} = super._renderItems(items, color, lineColor, viewport, cache, coordinateSystem);
        if (!coordinateSystem.isHeatMap) {
            const padding = 0.5;
            const barOrders = ['A', 'C', 'G', 'T', 'N'];
            for (let i = 0; i < items.length; i++) {
                for (let j = 0; j < items[i].points.length; j++) {
                    const point = items[i].points[j];
                    if (point.dataItem.isHighlightedLocus) {
                        const max = this._getYValue(point.dataValue, coordinateSystem) + 1;
                        const min = this._getYValue(cache.baseAxis, coordinateSystem) - 1;
                        let prevPercentage = 0;
                        for (let k = barOrders.length - 1; k >= 0; k--) {
                            let color = this._bamConfig.colors[barOrders[k]];
                            if (point.dataItem.locusLetter.toLowerCase() === barOrders[k].toLowerCase()) {
                                color = this._bamConfig.colors.base;
                            }
                            const value = point.dataItem.locusInfo && point.dataItem.locusInfo[barOrders[k].toLowerCase()] ?
                                point.dataItem.locusInfo[barOrders[k].toLowerCase()] : 0;
                            if (value === 0) {
                                continue;
                            }
                            const percentage = (value - cache.baseAxis) / (point.dataValue - cache.baseAxis);
                            const y1 = Math.round(Math.min(this.height - 1, min + (max - min) * (percentage + prevPercentage))) - .5;
                            const y2 = Math.round(Math.min(this.height - 1, min + (max - min) * prevPercentage)) + .5;
                            const x1 = Math.round(viewport.project.brushBP2pixel(point.startIndex - 0.5) + padding);
                            const x2 = Math.round(viewport.project.brushBP2pixel(point.endIndex + 0.5) - padding);
                            block.beginFill(color, 1);
                            block.lineStyle(1, color, 1);
                            block.moveTo(x1 + .5, y2);
                            block.lineTo(x1 + .5, y1);
                            block.lineTo(x2 - .5, y1);
                            block.lineTo(x2 - .5, y2);
                            block.lineTo(x1 + .5, y2);
                            block.endFill();
                            prevPercentage += percentage;
                        }
                    }
                }
            }
        }
        return {block, line};
    }
}