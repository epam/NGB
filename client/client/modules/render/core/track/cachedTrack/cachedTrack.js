import {ScrollableTrack} from '../scrollableTrack';
const Math = window.Math;

export default class DataTrack extends ScrollableTrack {
    cache: Object = {};
    dataConfig: Object;

    constructor(opts) {
        super(opts);
        Object.defineProperties(this, {
            dataConfig: {
                configurable: false,
                value: {
                    chromosomeId: opts.chromosomeId,
                    id: opts.openByUrl ? undefined : opts.id,
                    fileUrl: opts.openByUrl ? opts.id : undefined,
                    indexUrl: opts.openByUrl ? opts.indexPath : undefined,
                    openByUrl: opts.openByUrl,
                    projectId: opts.project ? opts.project.id : undefined,
                }
            }
        });
    }

    invalidateCache() {
        super.invalidateCache();
        if (this.cache) {
            this.cache.invalid = true;
        }
    }

    async getNewCache() {
        if (!this.viewport || this.viewport.factor <= 0)
            return false;

        if (this.cache !== undefined && this.cache !== null && !this.cache.invalid &&
            this.cache.viewport !== undefined && this.cache.viewport !== null &&
            this.cache.viewport.brush.start === this.viewport.brush.start &&
            this.cache.viewport.brush.end === this.viewport.brush.end) {
            return false;
        }

        return await this.updateCache();
    }

    cacheUpdateParameters(viewport) {
        if ((this.constructor.name === 'REFERENCETrack' || this.constructor.name === 'WIGTrack') && viewport.isShortenedIntronsMode) {
            const parametersArray = [];
            for (let i = 0; i < viewport.shortenedIntronsViewport._coveredRange.ranges.length; i++) {
                const range = viewport.shortenedIntronsViewport._coveredRange.ranges[i];
                const param = Object.assign({
                    endIndex: Math.round(Math.min(viewport.chromosomeSize, range.endIndex)),
                    scaleFactor: viewport.factor,
                    startIndex: Math.round(Math.max(1, range.startIndex))
                }, this.dataConfig);
                parametersArray.push(param);
            }
            return parametersArray;
        }
        return Object.assign({
            endIndex: Math.round(Math.min(viewport.chromosomeSize, viewport.brush.end + viewport.brushSize / 2)),
            scaleFactor: viewport.factor,
            startIndex: Math.round(Math.max(1, viewport.brush.start - viewport.brushSize / 2))
        }, this.dataConfig);
    }

    cacheUpdateInitialParameters(viewport) {
        return Object.assign({
            endIndex: viewport.chromosomeSize,
            scaleFactor: viewport.chromosomeFactor,
            startIndex: 1
        }, this.dataConfig);
    }

    async updateCache() {
        this.cache = Object.assign(this.cache,
            {
                dataViewport: {
                    endIndex: Math.round(Math.min(this.viewport.chromosomeSize,
                        this.viewport.brush.end + this.viewport.brushSize / 2)),
                    startIndex: Math.round(Math.max(1, this.viewport.brush.start - this.viewport.brushSize / 2))
                },
                invalid: false,
                isNew: true,
                viewport: {
                    brush: {
                        end: this.viewport.brush.end,
                        start: this.viewport.brush.start
                    },
                    factor: this.viewport.factor
                }
            });
        return true;
    }

    clearData() {
        super.clearData();
        this.cache = null;
    }

}
