import GeneFeatureAnalyzer from '../../../../../../dataServices/gene/gene-feature-analyzer';
import {GeneTransformer} from '../../../gene/internal';
import {Sorting} from '../../../../utilities';
import {scaleModes} from '../../../common/scaleModes';
import {displayModes} from '../../modes';

function reduceExtremum (result, current) {
    return {
        minimum: Math.min(result.minimum || 0, current.minimum || 0),
        maximum: Math.max(result.maximum || 0, current.maximum || 0)
    };
}

function extractValues (data, viewport) {
    const values = data || [];
    return values.filter(({startIndex, endIndex}) =>
        startIndex < viewport.brush.end && endIndex > viewport.brush.start
    )
        .map(v => v.value);
}

function getSourceExtremum (sourceData, viewport) {
    const viewportKey = `${Math.floor(viewport.brush.start)}-${Math.floor(viewport.brush.end)}`;
    if (sourceData) {
        const {
            __extremum__: extremum = {},
            data = []
        } = sourceData;
        if (extremum && extremum[viewportKey]) {
            return extremum[viewportKey];
        }
        const values = extractValues(data, viewport);
        const minimum = Math.min(...values);
        const maximum = Math.max(...values);
        if (!sourceData.__extremum__) {
            sourceData.__extremum__ = {};
        }
        sourceData.__extremum__[viewportKey] = {
            minimum,
            maximum
        };
        return sourceData.__extremum__[viewportKey];
    }
    return {
        minimum: 0,
        maximum: 1
    };
}

export class FeatureCountsTransformer extends GeneTransformer {
    transformData(data) {
        let genes = [];
        const unmappedFeatures = [];
        for (let i = 0; i < data.length; i++) {
            if (data[i].mapped !== undefined && data[i].mapped !== null && !data[i].mapped) {
                unmappedFeatures.push(data[i]);
            } else if (data[i].feature !== null && data[i].feature !== undefined && data[i].feature.toLowerCase() === 'gene') {
                data[i].name = data[i].name || GeneFeatureAnalyzer.getFeatureName(
                    data[i],
                    'gene_name',
                    'gene_symbol',
                    'gene_id'
                );
                genes.push(data[i]);
            }
        }
        for (let i = 0; i < unmappedFeatures.length; i++) {
            const item = GeneFeatureAnalyzer.updateFeatureName(unmappedFeatures[i]);
            if (item !== null) {
                genes.push(item);
            }
        }
        genes = Sorting.quickSort(genes, false, item => item.endIndex - item.startIndex);
        return genes;
    }

    getSourcesInfo (data, visibleSources = []) {
        const sources = new Set();
        const values = {};
        (data || []).forEach(item => {
            const attributeNames = Object.keys(item.attributes || {})
                .filter(name => /\.bam$/i.test(name));
            attributeNames.forEach(name => {
                if (!values.hasOwnProperty(name)) {
                    values[name] = {
                        data: [],
                        disabled: visibleSources &&
                            visibleSources.length > 0 &&
                            !visibleSources.includes(name)
                    };
                }
                sources.add(name);
                const value = item.attributes[name];
                if (!Number.isNaN(Number(value)) && Number(value)) {
                    values[name].data.push({
                        startIndex: item.startIndex,
                        endIndex: item.endIndex,
                        value: Number(value)
                    });
                }
            });
        });
        if (this.groupAutoScaleManager) {
            this.groupAutoScaleManager.registerTrackData(
                this.track,
                values
            );
        }
        return {
            sources: [...sources].sort(),
            values
        };
    }

    getCoordinateSystem (viewport, cache, state) {
        const {
            featureCountsDisplayMode,
            coverageLogScale,
            coverageScaleMode,
            coverageScaleFrom,
            coverageScaleTo,
            groupAutoScale
        } = state;
        if (featureCountsDisplayMode === displayModes.barChart) {
            const extend = (minimum, maximum, options) => {
                const {
                    minimum: extendMinimum = false,
                    maximum: extendMaximum = true,
                    log = false,
                    extendRatio = 0.2
                } = options;
                if (log) {
                    const log10 = o => o > 0 ? Math.log10(o) : 0;
                    return {
                        minimum: extendMinimum
                            ? (10 ** (Math.floor(log10(minimum)) - extendRatio))
                            : minimum,
                        maximum: extendMaximum
                            ? (10 ** (Math.ceil(log10(maximum)) + extendRatio))
                            : maximum
                    };
                } else {
                    let range = maximum - minimum;
                    if (range === 0) {
                        range = 1;
                    }
                    return {
                        minimum: minimum - (extendMinimum ? range * extendRatio : 0),
                        maximum: maximum + (extendMaximum ? range * extendRatio : 0)
                    };
                }
            };
            const values = cache.sources
                ? (cache.sources.values || {})
                : {};
            const getSourcesExtremum = (dataArray) => dataArray
                .map(data => Object.values(data || {}))
                .reduce((r, c) => ([...r, ...c]), [])
                .filter(sourceData => !sourceData.disabled)
                .map(sourceData => getSourceExtremum(sourceData, viewport))
                .reduce(reduceExtremum, {
                    minimum: Infinity,
                    maximum: -Infinity
                });
            const log = !!coverageLogScale;
            switch (coverageScaleMode) {
                case scaleModes.manualScaleMode:
                    return {
                        ...extend(
                            coverageScaleFrom,
                            coverageScaleTo,
                            {
                                minimum: false,
                                maximum: false,
                                log
                            }
                        ),
                        log
                    };
                case scaleModes.groupAutoScaleMode:
                case scaleModes.defaultScaleMode:
                default: {
                    const dataArray = coverageScaleMode === scaleModes.groupAutoScaleMode &&
                    this.groupAutoScaleManager
                        ? this.groupAutoScaleManager.getGroupData(groupAutoScale)
                        : [values];
                    const e = getSourcesExtremum(dataArray);
                    let {minimum} = e;
                    const {maximum} = e;
                    if (!log) {
                        minimum = Math.min(minimum, 0);
                    }
                    const extremum = extend(
                        minimum,
                        maximum,
                        {
                            minimum: log,
                            log
                        }
                    );
                    return {
                        ...extremum,
                        log: !!coverageLogScale,
                        groupAutoScale
                    };
                }
            }
        }
        return undefined;
    }
}
