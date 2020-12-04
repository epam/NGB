import {NumberFormatter, Sorting} from '../../utilities';
import {displayModes, scaleModes} from './modes';
const Math = window.Math;

export default class WIGTransformer {
    constructor(config) {
        this._config = config;
    }

    _getExtremumValues(wigData, viewport) {

        let maximum = this._config.area.maximum;
        let minimum = this._config.area.minimum || 0;

        for (let i = 0; i < wigData.extremumSupportStructure.length; i++) {
            const item = wigData.extremumSupportStructure[i];
            if (item.startIndex <= viewport.brush.end && item.endIndex >= viewport.brush.start) {
                maximum = item.value;
                break;
            }
        }

        for (let i = 0; i < wigData.minExtremumSupportStructure.length; i++) {
            const item = wigData.minExtremumSupportStructure[i];
            if (item.startIndex <= viewport.brush.end && item.endIndex >= viewport.brush.start) {
                minimum = item.value;
                break;
            }
        }

        return {maximum, minimum: Math.min(0, minimum)};
    }

    transformCoordinateSystem(wigData, viewport, cachedCoordinateSystem, coverageConfig) {
        const {
            coverageDisplayMode,
            coverageScaleMode,
            coverageLogScale,
            coverageScaleFrom,
            coverageScaleTo
        } = coverageConfig;
        if (wigData === null)
            return;
        let maximum = null;
        let minimum = 0; // by default
        const extremumValues = this._getExtremumValues(wigData, viewport);
        switch (coverageScaleMode) {
            case scaleModes.manualScaleMode: {
                maximum = !isNaN(coverageScaleTo) ? +coverageScaleTo : extremumValues.maximum;
                minimum = !isNaN(coverageScaleTo) ? +coverageScaleFrom : extremumValues.minimum;
            } break;
            default: {
                maximum = extremumValues.maximum;
                minimum = extremumValues.minimum;
            } break;
        }

        const realMaximum = extremumValues.maximum;
        const realMinimum = extremumValues.minimum;

        if (coverageLogScale) {
            maximum = maximum > 0 ? Math.ceil(Math.log(maximum) / Math.log(10)) : 0;
            minimum = minimum > 0 ? (Math.log(minimum) / Math.log(10)): 0;
        }

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
            isHeatMap: coverageDisplayMode === displayModes.heatMapDisplayMode,
            isLogScale: coverageLogScale,
            maximum: maximum,
            minimum: minimum,
            realMaximum: realMaximum,
            realMinimum: realMinimum
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

        const _baseAxis = 0;
        const extremumSupportStructure = [];
        const minExtremumSupportStructure = [];

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
            return item.value === 0 || item.startIndex > allowedEnd || item.endIndex < allowedStart ||
                (viewport.isShortenedIntronsMode && viewport.shortenedIntronsViewport.shouldSkipFeature(item));
        };

        let lastItem = null;

        let prevEndIndexPx = null;

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

            extremumSupportStructure.push({
                value: dataItem.value,
                startIndex: dataItem.startIndex,
                endIndex: dataItem.endIndex
            });
            minExtremumSupportStructure.push({
                value: dataItem.value,
                startIndex: dataItem.startIndex,
                endIndex: dataItem.endIndex
            });

            const dataItemThreshold = this.isThresholdValue(dataItem.value);

            if (lastItem && (lastItem.endIndex + 1 === dataItem.startIndex || lastItem.endIndex === dataItem.startIndex)) {
                if (lastItem.threshold !== dataItemThreshold ||
                    (lastItem.value - _baseAxis) * (dataItem.value - _baseAxis) < 0) {
                    pushItem(lastItem);
                    lastItem = null;
                }
                else {
                    if (!isDetailed && dataItem.startIndex === dataItem.endIndex && prevEndIndexPx !== null && lastItem.points.length > 0) {
                        if (Math.round(viewport.project.brushBP2pixel(dataItem.endIndex + 0.5)) === prevEndIndexPx) {
                            lastItem.endIndex = dataItem.endIndex;
                            const startIndex = lastItem.points[lastItem.points.length - 1].startIndex;
                            const endIndex = dataItem.endIndex;
                            if (!lastItem.points[lastItem.points.length - 1].dataItem.isHighlightedLocus) {
                                lastItem.points[lastItem.points.length - 1].dataItem = dataItem;
                                lastItem.points[lastItem.points.length - 1].dataValue = Math.max(dataItem.value, lastItem.points[lastItem.points.length - 1].dataValue);
                            }
                            lastItem.points[lastItem.points.length - 1].startIndex = startIndex;
                            lastItem.points[lastItem.points.length - 1].endIndex = endIndex;
                            continue;
                        }
                    }
                    prevEndIndexPx = Math.round(viewport.project.brushBP2pixel(dataItem.endIndex + 0.5));
                    lastItem.points.push(
                        {
                            dataItem: dataItem,
                            dataValue: dataItem.value,
                            startIndex: dataItem.startIndex,
                            endIndex: dataItem.endIndex
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

            prevEndIndexPx = Math.round(viewport.project.brushBP2pixel(dataItem.endIndex + .5));

            lastItem = {
                startIndex: dataItem.startIndex,
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
                            startIndex: i,
                            endIndex: i
                        }
                    );
                }
            }
            else {
                lastItem.points.push(
                    {
                        dataItem: dataItem,
                        dataValue: dataItem.value,
                        startIndex: dataItem.startIndex,
                        endIndex: dataItem.endIndex
                    }
                );
            }
        }
        pushItem(lastItem);
        Sorting.quickSort(extremumSupportStructure, false, x => x.value);
        Sorting.quickSort(minExtremumSupportStructure, true, x => x.value);
        return {
            baseAxis: _baseAxis,
            dataItems: data,
            extremumSupportStructure: extremumSupportStructure,
            minExtremumSupportStructure: minExtremumSupportStructure,
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
