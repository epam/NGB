import {GeneDataService} from './gene-data-service';

class CachedGeneDataService extends GeneDataService {
    static loadGeneTrackCache = new Map();
    static loadGeneHistogramTrackCache = new Map();

    loadGeneTrackCacheArgs(gene, referenceId) {
        const {
            chromosomeId,
            collapsed,
            endIndex,
            fileUrl,
            id,
            indexUrl,
            openByUrl,
            projectId,
            scaleFactor,
            startIndex,
        } = gene;
        const key = [
            referenceId,
            id,
            projectId,
            openByUrl,
            fileUrl,
            indexUrl,
        ]
            .map(o => `${o || ''}`)
            .join('|');
        const hash = [
            chromosomeId,
            !!collapsed,
            scaleFactor,
            startIndex,
            endIndex
        ]
            .map(o => `${o || ''}`)
            .join('|');
        return {key, hash};
    }

    loadGeneHistogramTrackCacheArgs(gene) {
        const {
            chromosomeId,
            endIndex,
            id,
            projectId,
            scaleFactor,
            startIndex,
            openByUrl
        } = gene;
        if (openByUrl) {
            return undefined;
        }
        const key = [
            id,
            projectId
        ]
            .map(o => `${o || ''}`)
            .join('|');
        const hash = [
            chromosomeId,
            scaleFactor,
            startIndex,
            endIndex
        ]
            .map(o => `${o || ''}`)
            .join('|');
        return {key, hash};
    }

    loadGeneTrack(gene, referenceId, update = false) {
        const args = this.loadGeneTrackCacheArgs(gene, referenceId);
        if (!args) {
            return super.loadGeneTrack(gene, referenceId);
        }
        const {key, hash} = args;
        if (!update) {
            let cache;
            if (this.constructor.loadGeneTrackCache.has(key)) {
                cache = this.constructor.loadGeneTrackCache.get(key);
            }
            if (cache && cache.hash === hash) {
                return cache.promise;
            }
        }
        const promise = super.loadGeneTrack(gene, referenceId);
        this.constructor.loadGeneTrackCache.set(key, {hash, promise});
        return promise;
    }

    loadGeneHistogramTrack(gene) {
        const args = this.loadGeneHistogramTrackCacheArgs(gene);
        if (!args) {
            return super.loadGeneHistogramTrack(gene);
        }
        const {key, hash} = args;
        let cache;
        if (this.constructor.loadGeneHistogramTrackCache.has(key)) {
            cache = this.constructor.loadGeneHistogramTrackCache.get(key);
        }
        if (cache && cache.hash === hash) {
            return cache.promise;
        }
        const promise = super.loadGeneHistogramTrack(gene);
        this.constructor.loadGeneHistogramTrackCache.set(key, {hash, promise});
        return promise;
    }
}

export {CachedGeneDataService};
