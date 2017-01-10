import {NumberFormatter} from '../../utilities';
const Math = window.Math;

export default class WIGTransformer {
    constructor(config) {
        this._config = config;
    }

    _getExtremumValues(wigData, viewport) {
        let minimum = this._config.area.minimum;
        let maximum = this._config.area.maximum;
        const allowedStart = viewport.project.pixel2brushBP(-viewport.canvasSize);
        const allowedEnd = viewport.project.pixel2brushBP(2 * viewport.canvasSize);

        const skipItem = function(item) {
            return item.startIndex < viewport.brush.start || item.endIndex > viewport.brush.end ||
                item.startIndex > allowedEnd || item.endIndex < allowedStart;
        };

        for (let i = 0; i < wigData.dataItems.length; i++) {
            const item = wigData.dataItems[i];
            if (skipItem(item)) {
                continue;
            }
            if (minimum === null || minimum === undefined || minimum > item.value) {
                minimum = item.value;
            }
            if (maximum === null || maximum === undefined || maximum < item.value) {
                maximum = item.value;
            }
        }
        return {maximum, minimum};
    }

    transformCoordinateSystem(wigData, viewport, cachedCoordinateSystem) {
        if (wigData === null)
            return;
        let {maximum, minimum} = this._getExtremumValues(wigData, viewport);

        const diff = maximum - minimum;
        let dividers = [];

        if ((minimum === null || maximum === null || minimum === maximum) && cachedCoordinateSystem) {
            // Area of interest does not contains data.
            // For preventing empty area (because of null or equal min & max),
            // we will display previous area settings (min, max & dividers).
            // At the same time, previous setting could be null or zero only if
            // no data available for particular wigArea.
            minimum = cachedCoordinateSystem.minimum;
            maximum = cachedCoordinateSystem.maximum;
            dividers = cachedCoordinateSystem.dividers;
        }
        else {
            const closestDecimalDegree = NumberFormatter.findClosestDecimalDegree(diff, this._config.area.dividers);
            const decimalBase = 10;
            const module = Math.pow(decimalBase, closestDecimalDegree);
            const subModule = module / 2;
            const prevModule = Math.pow(decimalBase, closestDecimalDegree - 1);
            const step = WIGTransformer.findBestStep(diff / (this._config.area.dividers + 1), subModule);
            let center = minimum + diff / 2;
            center = Math.round(center / module) * module;
            if (minimum * maximum <= 0) {
                center = 0;
            }
            dividers = WIGTransformer._buildDividerValues(step, center, maximum, minimum, prevModule);
            // correction - trying to decrease dividers count to match requested number (this._config.area.dividers).
            while (dividers.length > this._config.area.dividers * 2) {
                dividers = WIGTransformer._buildDividerValues(step * 2, center, maximum, minimum, prevModule);
            }
            const rangeFactor = 0.1;
            maximum += diff * rangeFactor;
        }
        return {
            dividers: dividers,
            maximum: maximum,
            minimum: minimum
        };
    }

    static _buildDividerValues(step, center, maximum, minimum, module) {
        const dividers = [];
        dividers.push({value: center});

        let divider = center + step;

        while (divider < maximum - step) {
            dividers.push({value: (Math.round(divider / module) * module)});
            divider += step;
        }

        divider = center - step;
        while (divider > minimum) {
            dividers.push({value: (Math.round(divider / module) * module)});
            divider -= step;
        }
        return dividers;
    }

    transform(data, viewport) {
        const pixelsPerBp = viewport.factor;
        const isDetailed = pixelsPerBp >= this._config.wig.detailedStyleStartingAtPixelsPerBP;

        const allowedStart = viewport.project.pixel2brushBP(-viewport.canvasSize);
        const allowedEnd = viewport.project.pixel2brushBP(2 * viewport.canvasSize);

        const items = {
            aboveBaseAxis: [],
            belowBaseAxis: []
        };
        const thresholdItems = {
            aboveBaseAxis: [],
            belowBaseAxis: []
        };

        const pushItem = function(item) {
            if (item && item.threshold) {
                if (item.value > _baseAxis) {
                    thresholdItems.aboveBaseAxis.push(item);
                } else {
                    thresholdItems.belowBaseAxis.push(item);
                }
            }
            else if (item) {
                if (item.value > _baseAxis) {
                    items.aboveBaseAxis.push(item);
                } else {
                    items.belowBaseAxis.push(item);
                }
            }
        };

        const skipItem = function(item) {
            return item.startIndex > allowedEnd || item.endIndex < allowedStart ||
                (viewport.isShortenedIntronsMode && viewport.shortenedIntronsViewport.shouldSkipFeature(item));
        };

        let lastItem = null;

        const _baseAxis = 0;

        for (let index = 0; index < data.length; index++) {
            const dataItem = data[index];
            if (skipItem(dataItem)) {
                continue;
            }
            this.transformItem(dataItem);
            dataItem.startIndex = Math.max(allowedStart, dataItem.startIndex);
            dataItem.endIndex = Math.min(allowedEnd, dataItem.endIndex);
            if (dataItem.value === _baseAxis) {
                pushItem(lastItem);
                lastItem = null;
                continue;
            }

            const dataItemThreshold = this.isThresholdValue(dataItem.value);

            if (lastItem && lastItem.endIndex + 1 === dataItem.startIndex) {
                if (lastItem.threshold !== dataItemThreshold ||
                    (lastItem.value - _baseAxis) * (dataItem.value - _baseAxis) < 0) {
                    pushItem(lastItem);
                    lastItem = null;
                }
                else {
                    lastItem.points.push(
                        {
                            dataItem: dataItem,
                            dataValue: dataItem.value,
                            xEnd: viewport.project.brushBP2pixel(dataItem.endIndex),
                            xStart: viewport.project.brushBP2pixel(dataItem.startIndex)
                        }
                    );
                    lastItem.endIndex = dataItem.endIndex;
                    continue;
                }
            }
            else if (lastItem && lastItem.endIndex + 1 < dataItem.startIndex) {
                pushItem(lastItem);
                lastItem = null;
            }

            lastItem = {
                endIndex: dataItem.endIndex,
                points: [],
                threshold: dataItemThreshold,
                value: dataItem.value
            };
            if (isDetailed) {
                for (let i = dataItem.startIndex; i <= dataItem.endIndex; i++) {
                    lastItem.points.push(
                        {
                            dataItem: dataItem,
                            dataValue: dataItem.value,
                            xEnd: viewport.project.brushBP2pixel(i),
                            xStart: viewport.project.brushBP2pixel(i)
                        }
                    );
                }
            }
            else {
                lastItem.points.push(
                    {
                        dataItem: dataItem,
                        dataValue: dataItem.value,
                        xEnd: viewport.project.brushBP2pixel(dataItem.endIndex),
                        xStart: viewport.project.brushBP2pixel(dataItem.startIndex)
                    }
                );
            }
        }
        pushItem(lastItem);

        return {
            baseAxis: _baseAxis,
            dataItems: data,
            isDetailed: isDetailed,
            items: items,
            pixelsPerBp: pixelsPerBp,
            thresholdItems: thresholdItems,
            viewport: viewport
        };
    }

    static findBestStep(value, module) {
        const v1 = Math.floor(value / module) * module;
        const v2 = v1 + module;

        const d2 = (v2 - value) / module;
        const moduleAcceptanceFactor = 0.3;
        // 30% of module - acceptable.
        if (d2 < moduleAcceptanceFactor) {
            return v2;
        }
        return v1;
    }

    isThresholdValue(value) {
        if (this._config.area.thresholdMin === null && this._config.area.thresholdMax === null) {
            return false;
        }
        else if (this._config.area.thresholdMin !== null && this._config.area.thresholdMax !== null) {
            return value >= this._config.area.thresholdMin && value <= this._config.area.thresholdMax;
        }
        else if (this._config.area.thresholdMin !== null) {
            return value >= this._config.area.thresholdMin;
        }
        else if (this._config.area.thresholdMax !== null) {
            return value <= this._config.area.thresholdMax;
        }
        return false;
    }

    transformItem() {
    }
}
