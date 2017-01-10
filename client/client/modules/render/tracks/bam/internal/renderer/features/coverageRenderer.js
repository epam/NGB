import WIGRenderer from '../../../../wig/wigRenderer';

const Math = window.Math;

export class CoverageRenderer extends WIGRenderer {

    _bamConfig;

    constructor(config, bamConfig) {
        super(config);
        this._bamConfig = bamConfig;
    }

    _renderItems(items, color, viewport, cache, coordinateSystem) {
        const block = super._renderItems(items, color, viewport, cache, coordinateSystem);
        const pixelsPerBp = viewport.factor;
        const paddingDiff = 0.5;
        const padding = pixelsPerBp / 2.0 - paddingDiff;
        const barOrders = ['A', 'C', 'G', 'T', 'N'];
        for (let i = 0; i < items.length; i++) {
            for (let j = 0; j < items[i].points.length; j++) {
                const point = items[i].points[j];
                if (point.dataItem.isHighlightedLocus) {
                    let __y = cache.baseAxis;
                    for (let k = barOrders.length - 1; k >= 0; k--) {
                        const color = this._bamConfig.colors[barOrders[k]];
                        const value = point.dataItem.locusInfo && point.dataItem.locusInfo[barOrders[k].toLowerCase()] ?
                            point.dataItem.locusInfo[barOrders[k].toLowerCase()] : 0;
                        block.beginFill(color, 1);
                        block.drawRect(
                            Math.floor(this.correctedXPosition(point.xStart) - padding),
                            Math.floor(this._getYValue(__y, coordinateSystem)),
                            Math.max(1, this.correctedXPosition(point.xEnd) - this.correctedXPosition(point.xStart) +
                                2 * padding),
                            this._getYValue(__y + value, coordinateSystem) - this._getYValue(__y, coordinateSystem)
                        );
                        block.endFill();
                        __y += value;
                    }
                }
            }
        }
        return block;
    }
}