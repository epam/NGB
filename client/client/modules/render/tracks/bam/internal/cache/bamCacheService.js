import BamCache from './bamCache';
import {BamDataService} from '../../../../../../dataServices';
import {CoverageTransformer} from '../transformers';
import {dataModes} from '../../modes';

const Math = window.Math;

export default class BamCacheService {

    dataService;

    _properties = {
        maxAlignmentsRange: null,
        maxCoverageRange: null,
        rendering: null,
        request: null
    };

    _cache: BamCache;

    get cache(): BamCache {
        return this._cache;
    }

    constructor(track, config, cache) {
        this.dataService = new BamDataService(config.dispatcher);
        this._cache = cache || new BamCache(track);
        this._config = config;
        this._coverageTransformer = new CoverageTransformer(this._cache, config);
        this._coverageTransformer.registerGroupAutoScaleManager(config.groupAutoScaleManager, track);
        if (!cache) {
            this.cache.invalidate();
        }
    }

    get properties() {
        return this._properties;
    }

    set properties(props) {
        this.cache.invalidate();
        this._properties = Object.assign(this._properties, props || {});
        this.cache.renderingProperties = this._properties.rendering;
    }

    getRequestBoundaries(viewport) {
        const boundaries = {
            endIndex: Math.min(
                Math.ceil(viewport.brush.end + viewport.brushSize * this._config.requestPreCache),
                viewport.chromosomeSize
            ),
            startIndex: Math.max(
                Math.floor(viewport.brush.start - viewport.brushSize * this._config.requestPreCache),
                1
            )
        };
        if (viewport.isShortenedIntronsMode) {
            boundaries.startIndex = Math.floor(viewport.shortenedIntronsViewport
                .translatePosition(viewport.brush.start, - viewport.shortenedIntronsViewport.brush.shortenedSize));
            boundaries.endIndex = Math.ceil(viewport.shortenedIntronsViewport
                .translatePosition(viewport.brush.end, viewport.shortenedIntronsViewport.brush.shortenedSize));
        }
        return boundaries;
    }

    async _completeCacheWhenInvalid(viewport, boundaries, features, callbacks) {
        const {onStart} = callbacks;
        this.cache
            .invalidate()
            .startUpdate(boundaries);
        if (onStart) {
            onStart();
        }
        const dataRequestParameters = BamCacheService.getUpdateParameters(viewport, boundaries);
        let data = await this.createDataRequest(dataRequestParameters.length === 1 ? dataRequestParameters[0] : dataRequestParameters);
        await this.cache.append(data, features);
        data = null;
        return this.cache.endUpdate(viewport, this._coverageTransformer, features);
    }

    async _renewCache(viewport, boundaries, features, callbacks) {
        const {onStart} = callbacks;
        this.cache
            .invalidate(true)
            .startUpdate(boundaries);
        if (onStart) {
            onStart();
        }
        const dataRequestParameters = BamCacheService.getUpdateParameters(viewport, boundaries);
        let data = await this.createDataRequest(dataRequestParameters.length === 1 ? dataRequestParameters[0] : dataRequestParameters);
        await this.cache.append(data, features);
        data = null;
        return this.cache.endUpdate(viewport, this._coverageTransformer, features);
    }

    static getUpdateParameters(viewport, boundaries) {
        const dataRequestParameters = [];
        if (viewport.isShortenedIntronsMode) {
            for (let i = 0; i < viewport.shortenedIntronsViewport._coveredRange.ranges.length; i++) {
                const range = viewport.shortenedIntronsViewport._coveredRange.ranges[i];
                if (boundaries.endIndex < range.startIndex || boundaries.startIndex > range.endIndex) {
                    continue;
                }
                dataRequestParameters.push(
                    {
                        endIndex: Math.min(boundaries.endIndex, range.endIndex),
                        side: 'center',
                        startIndex: Math.max(boundaries.startIndex, range.startIndex),
                        scaleFactor: viewport.factor
                    });
            }
        } else {
            dataRequestParameters.push(
                {
                    endIndex: boundaries.endIndex,
                    side: 'center',
                    startIndex: boundaries.startIndex,
                    scaleFactor: viewport.factor
                });
        }
        return dataRequestParameters;
    }

    static getLeftUpdateParameters(endIndex, viewport, boundaries) {
        const dataRequestParameters = [];
        if (viewport.isShortenedIntronsMode) {
            for (let i = 0; i < viewport.shortenedIntronsViewport._coveredRange.ranges.length; i++) {
                const range = viewport.shortenedIntronsViewport._coveredRange.ranges[i];
                if (endIndex < range.startIndex || boundaries.startIndex > range.endIndex) {
                    continue;
                }
                if (range.endIndex < endIndex) {
                    dataRequestParameters.push(
                        {
                            endIndex: range.endIndex,
                            side: 'center',
                            startIndex: Math.max(boundaries.startIndex, range.startIndex),
                            scaleFactor: viewport.factor
                        });
                } else {
                    dataRequestParameters.push(
                        {
                            endIndex: endIndex,
                            side: 'left',
                            startIndex: Math.max(boundaries.startIndex, range.startIndex),
                            scaleFactor: viewport.factor
                        });
                }
            }
        } else {
            dataRequestParameters.push(
                {
                    endIndex: endIndex,
                    side: 'left',
                    startIndex: boundaries.startIndex,
                    scaleFactor: viewport.factor
                });
        }
        return dataRequestParameters;
    }

    static getRightUpdateParameters(startIndex, viewport, boundaries) {
        const dataRequestParameters = [];
        if (viewport.isShortenedIntronsMode) {
            for (let i = 0; i < viewport.shortenedIntronsViewport._coveredRange.ranges.length; i++) {
                const range = viewport.shortenedIntronsViewport._coveredRange.ranges[i];
                if (boundaries.endIndex < range.startIndex || startIndex > range.endIndex) {
                    continue;
                }
                if (range.startIndex > startIndex) {
                    dataRequestParameters.push(
                        {
                            endIndex: Math.min(boundaries.endIndex, range.endIndex),
                            side: 'center',
                            startIndex: range.startIndex,
                            scaleFactor: viewport.factor
                        });
                } else {
                    dataRequestParameters.push(
                        {
                            endIndex: Math.min(boundaries.endIndex, range.endIndex),
                            side: 'right',
                            startIndex: startIndex,
                            scaleFactor: viewport.factor
                        });
                }
            }
        } else {
            dataRequestParameters.push(
                {
                    endIndex: boundaries.endIndex,
                    side: 'right',
                    startIndex: startIndex,
                    scaleFactor: viewport.factor
                });
        }
        return dataRequestParameters;
    }

    async completeCacheData(viewport, features, callbacks) {
        if (this.cache.isUpdating) {
            return false;
        }
        const {onStart} = callbacks;
        const boundaries = this.getRequestBoundaries(viewport);
        const cacheCompletenessResult = this.cache.check(boundaries);
        if (cacheCompletenessResult.cacheIsComplete && this.cache.dataMode === dataModes.full) {
            return false;
        }
        if (cacheCompletenessResult.dataModeChanged || this.cache.dataMode !== dataModes.full) {
            return this._renewCache(viewport, boundaries, features, callbacks);
        } else if (cacheCompletenessResult.cacheIsInvalid) {
            return this._completeCacheWhenInvalid(viewport, boundaries, features, callbacks);
        } else {
            this.cache.startUpdate(boundaries);
            if (onStart) {
                onStart();
            }
            if (cacheCompletenessResult.needUpdateLeftSide) {
                const dataRequestParameters = BamCacheService.getLeftUpdateParameters(this.cache.coverageRange.startIndex - 1,
                    viewport, boundaries);
                let data = await this.createDataRequest(dataRequestParameters.length === 1 ?
                    dataRequestParameters[0] : dataRequestParameters);
                this.cache.appendLeft(data, features);
                data = null;
            }
            if (cacheCompletenessResult.needUpdateRightSide) {
                const dataRequestParameters = BamCacheService.getRightUpdateParameters(this.cache.coverageRange.endIndex + 1, viewport, boundaries);
                let data = await this.createDataRequest(dataRequestParameters.length === 1 ? dataRequestParameters[0] : dataRequestParameters);
                this.cache.appendRight(data, features);
                data = null;
            }
            return this.cache.endUpdate(viewport, this._coverageTransformer, features);
        }
    }

    transform(viewport, scaleConfig){
        this.cache.updateCoverageData(viewport, this._coverageTransformer, scaleConfig);
    }

    createDataRequest(props) {
        const filter = {
            failedVendorChecks: this.properties.rendering.filterFailedVendorChecks,
            pcrOpticalDuplicates: this.properties.rendering.filterPcrOpticalDuplicates,
            secondaryAlignments: this.properties.rendering.filterSecondaryAlignments,
            supplementaryAlignments: this.properties.rendering.filterSupplementaryAlignments
        };

        if (props instanceof Array) {
            const requestParameters = [];
            for (let i = 0; i < props.length; i++) {
                requestParameters.push({...props[i], ...this.properties.request, mode: this.cache.dataMode});
            }
            return this.dataService.getReads(requestParameters, this.properties.rendering.isSoftClipping, filter);
        } else {
            return this.dataService.getReads({...props, ...this.properties.request, mode: this.cache.dataMode}, this.properties.rendering.isSoftClipping, filter);
        }
    }

    clear() {
        this.cache.invalidate();
    }

    clone() {
        const cache = this._cache.clone();
        if (!cache) {
            return undefined;
        }
        const cloned = new BamCacheService(cache.track, this._config, cache);
        cloned._properties = this._properties;
        return cloned;
    }
}
