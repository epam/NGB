import BamCache from '../cache/bamCache';
import WIGTransformer from '../../../wig/wigTransformer';

const Math = window.Math;

export default class BamReadsCoverageTransformer extends WIGTransformer {

    _bamCache: BamCache;

    constructor(bamCache: BamCache, config) {
        super(config);
        this._bamCache = bamCache;
    }

    static getTooltipContent(item, displayOnlyCoverageInfo = false) {
        const percentageBase = 100;
        const percentageFractionBase = 10000;
        const getCell = function(value) {
            return `${value} (${Math.ceil(value / item.value * percentageFractionBase) / percentageBase}%)`;
        };

        if (!item.locusInfo) {
            if (displayOnlyCoverageInfo) {
                return [
                    ['Count', Math.ceil(item.value)]
                ];
            } else {
                return [
                    ['Count', Math.ceil(item.value)],
                    ['DEL:', item.delCov || 0],
                    ['INS:', item.insCov || 0]
                ];
            }
        }

        return [
            ['Count', Math.ceil(item.value)],
            ['A:', getCell(item.locusInfo.a)],
            ['C:', getCell(item.locusInfo.c)],
            ['G:', getCell(item.locusInfo.g)],
            ['T:', getCell(item.locusInfo.t)],
            ['N:', getCell(item.locusInfo.n)],
            ['DEL:', item.delCov || 0],
            ['INS:', item.insCov || 0]
        ];
    }

    transformItem(item) {
        super.transformItem(item);
        if (item.isTransformed)
            return;
        const highlightThreshold = 0.9;
        item.aCov = item.aCov || 0;
        item.cCov = item.cCov || 0;
        item.gCov = item.gCov || 0;
        item.nCov = item.nCov || 0;
        item.tCov = item.tCov || 0;

        item.totalMismatches = item.aCov + item.cCov + item.gCov + item.tCov + item.nCov;
        item.isHighlightedLocus = (item.value - item.totalMismatches) / item.value <= highlightThreshold;
        item.isTransformed = true;
        const locusLetter = this._bamCache.getReferenceValueAtLocus(item.startIndex);
        let locusInfo = {
            a: item.aCov,
            c: item.cCov,
            g: item.gCov,
            n: item.nCov,
            t: item.tCov
        };
        if (locusLetter && locusInfo[locusLetter.toLowerCase()] !== undefined) {
            locusInfo[locusLetter.toLowerCase()] = item.value - item.totalMismatches;
        }
        else {
            locusInfo = null;
        }

        item.locusInfo = locusInfo;
    }
}