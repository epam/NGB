import * as actions from './actions';
import * as cachePositions from './bamCachePositions';
import {dataModes, groupModes} from '../../modes';
import {layoutReads, transform} from '../transformers';
import FastSet from 'collections/fast-set';
import {Line} from './line';
import {Sorting} from '../../../../utilities';

const Math = window.Math;

export default class BamCache {
    coverageRange = {};
    readNames = new FastSet();
    downsampleCoverage = [];
    coverageItems = [];
    regionItems = [];
    spliceJunctions = [];
    coverage = {coordinateSystem: null, data: null};
    regions = {coordinateSystem: null, data: null};
    _groups = [];
    _renderingProperties = {};
    _isUpdating = false;
    _updatingBoundaries = null;
    _shouldRearrangeReads = false;
    _referenceBuffers = [];

    _cacheUpdated = false;
    _groupMode = groupModes.defaultGroupingMode;
    _totalLines = 0;
    _totalPairedLines = 0;
    _dataMode;
    _dataModeChanged = false;
    _delayedInvalidateRequested = false;

    get linesCount() {
        return this.renderingProperties.viewAsPairs ? this._totalPairedLines : this._totalLines;
    }

    get groups() {
        return this._groups;
    }

    get dataMode() {
        return this._dataMode;
    }

    set dataMode(value) {
        this._dataModeChanged = value !== this._dataMode;
        this._dataMode = value;
    }

    rangeIsEmpty(viewport) {
        for (let g = 0; g < this._groups.length; g++) {
            if (this.renderingProperties.viewAsPairs) {
                for (let i = 0; i < this._groups[g].pairedLines.length; i++) {
                    if (!this._groups[g].pairedLines[i].rangeIsEmpty(viewport)) {
                        return {rangeIsEmpty: false, noRegions: false};
                    }
                }
            }
            else {
                for (let i = 0; i < this._groups[g].lines.length; i++) {
                    if (!this._groups[g].lines[i].rangeIsEmpty(viewport)) {
                        return {rangeIsEmpty: false, noRegions: false};
                    }
                }
            }
        }
        for (let c = 0; c < this.coverageItems.length; c++) {
            if ((this.coverageItems[c].startIndex >= viewport.brush.start && this.coverageItems[c].startIndex <= viewport.brush.end) ||
                (this.coverageItems[c].endIndex >= viewport.brush.start && this.coverageItems[c].endIndex <= viewport.brush.end) ||
                (this.coverageItems[c].startIndex < viewport.brush.start && this.coverageItems[c].endIndex > viewport.brush.end)) {
                return {rangeIsEmpty: false, noRegions: false};
            }
        }
        if (this.dataMode === dataModes.regions) {
            for (let c = 0; c < this.regionItems.length; c++) {
                if ((this.regionItems[c].startIndex >= viewport.brush.start && this.regionItems[c].startIndex <= viewport.brush.end) ||
                    (this.regionItems[c].endIndex >= viewport.brush.start && this.regionItems[c].endIndex <= viewport.brush.end) ||
                    (this.regionItems[c].startIndex < viewport.brush.start && this.regionItems[c].endIndex > viewport.brush.end)) {
                    return {rangeIsEmpty: true, noRegions: false};
                }
            }
        }
        return {rangeIsEmpty: true, noRegions: true};
    }

    getLine(index) {
        for (let g = 0; g < this._groups.length; g++) {
            if (this.renderingProperties.viewAsPairs) {
                if (index >= this._groups[g].previousPairedLinesCount + this._groups[g].pairedLines.length) {
                    continue;
                }
                return this._groups[g].pairedLines[index - this._groups[g].previousPairedLinesCount];
            }
            else {
                if (index >= this._groups[g].previousLinesCount + this._groups[g].lines.length) {
                    continue;
                }
                return this._groups[g].lines[index - this._groups[g].previousLinesCount];
            }
        }
        return null;
    }

    get groupMode() {
        return this._groupMode;
    }

    set groupMode(value) {
        const groupModeChanged = this._groupMode !== value;
        this._groupMode = value;
        if (groupModeChanged)
            this.group();
    }

    getReferenceValueAtLocus(locus) {
        for (let i = 0; i < this._referenceBuffers.length; i++) {
            if (this._referenceBuffers[i].startIndex <= locus && this._referenceBuffers[i].endIndex >= locus) {
                return this._referenceBuffers[i].reference[locus - this._referenceBuffers[i].startIndex];
            }
        }
        return null;
    }

    get renderingProperties() {
        return this._renderingProperties;
    }

    set renderingProperties(props) {
        this._renderingProperties = props;
    }

    get isUpdating() {
        return this._isUpdating;
    }

    constructor(track, invalidate = true) {
        this.track = track;
        if (invalidate) {
            this.invalidate();
        }
    }

    invalidate(delayed = false): BamCache {
        if (delayed) {
            this._delayedInvalidateRequested = true;
            return this;
        }
        this._delayedInvalidateRequested = false;
        this.coverageRange.startIndex = Infinity;
        this.coverageRange.endIndex = -Infinity;
        for (let i = 0; i < this._groups.length; i++) {
            this._groups[i].rawReads = [];
            this._groups[i].rawPairedReads = [];
            this._groups[i].lines = [];
            this._groups[i].pairedLines = [];
        }
        this.downsampleCoverage = [];
        this.coverageItems = [];
        this.regionItems = [];
        this.spliceJunctions = [];
        this._referenceBuffers = [];
        this.coverage = {coordinateSystem: null, data: null};
        if (this.readNames) {
            this.readNames.clear();
        }
        this.readNames = null;
        this._isUpdating = false;
        this._groups = [];
        return this;
    }

    check(boundaries) {
        const {startIndex, endIndex} = boundaries;
        const dataModeChanged = this._dataModeChanged;
        const needUpdateLeftSide = startIndex < this.coverageRange.startIndex - 1;
        const needUpdateRightSide = endIndex > this.coverageRange.endIndex + 1;
        const cacheIsComplete = !dataModeChanged && startIndex >= this.coverageRange.startIndex && endIndex <= this.coverageRange.endIndex;
        const cacheIsInvalid = startIndex > this.coverageRange.endIndex || endIndex < this.coverageRange.startIndex;
        return {cacheIsComplete, cacheIsInvalid, dataModeChanged, needUpdateLeftSide, needUpdateRightSide};
    }

    startUpdate(boundaries): BamCache {
        if (!this._isUpdating) {
            this._updatingBoundaries = boundaries;
        }
        this._isUpdating = true;
        this._cacheUpdated = false;
        return this;
    }

    endUpdate(viewport, coverageTransformer, features) {
        this._dataModeChanged = false;
        if (this._updatingBoundaries) {
            this.coverageRange.startIndex = this._updatingBoundaries.startIndex;
            this.coverageRange.endIndex = this._updatingBoundaries.endIndex;
            this._preprocess(features);
            this._updatingBoundaries = null;
        }
        if (this._shouldRearrangeReads) {
            this.rearrange(features);
        }
        this._shouldRearrangeReads = false;
        this.updateCoverageData(viewport, coverageTransformer, features);
        this.updateRegionsData();
        this._isUpdating = false;
        return this._cacheUpdated;
    }

    rearrange(features) {
        if (!features || features.alignments) {
            this.preparePairedReads();
            if (this.renderingProperties.viewAsPairs) {
                for (let g = 0; g < this._groups.length; g++) {
                    this._groups[g].pairedLines = [];
                }
                this.rearrangeAsPairReads();
            }
            else {
                for (let g = 0; g < this._groups.length; g++) {
                    this._groups[g].lines = [];
                }
                this.rearrangeReads();
            }
        }
    }

    _preprocessReads() {
        if (!this.readNames) {
            this.readNames = new FastSet();
        }
        for (let g = 0; g < this._groups.length; g++) {
            let result = [];
            for (let i = 0; i < this._groups[g].rawReads.length; i++) {
                if (this._groups[g].rawReads[i].startIndex > this._updatingBoundaries.endIndex ||
                    this._groups[g].rawReads[i].endIndex < this._updatingBoundaries.startIndex) {
                    const readName = `${this._groups[g].rawReads[i].name}-${this._groups[g].rawReads[i].startIndex}-${this._groups[g].rawReads[i].endIndex}-${this._groups[g].rawReads[i].tlen}`;
                    if (this.readNames.has(readName)) {
                        this.readNames.delete(readName);
                        continue;
                    }
                    continue;
                }
                result.push(this._groups[g].rawReads[i]);
            }
            this._groups[g].rawReads = result;
            result = null;
        }
    }

    _preprocessCoverage() {
        let _coverageItems = [];
        if (this.coverageItems) {
            for (let i = 0; i < this.coverageItems.length; i++) {
                if (this.coverageItems[i].startIndex > this._updatingBoundaries.endIndex ||
                    this.coverageItems[i].endIndex < this._updatingBoundaries.startIndex) {
                    continue;
                }
                _coverageItems.push(this.coverageItems[i]);
            }
        }
        this.coverageItems = _coverageItems;
        _coverageItems = null;
    }

    _preprocessRegions() {
        let _regionItems = [];
        if (this.regionItems) {
            for (let i = 0; i < this.regionItems.length; i++) {
                if (this.regionItems[i].startIndex > this._updatingBoundaries.endIndex ||
                    this.regionItems[i].endIndex < this._updatingBoundaries.startIndex) {
                    continue;
                }
                _regionItems.push(this.regionItems[i]);
            }
        }
        this.regionItems = _regionItems;
        _regionItems = null;
    }

    _preprocessSpliceJunctions() {
        let _spliceJunctions = [];
        for (let i = 0; i < this.spliceJunctions.length; i++) {
            if (this.spliceJunctions[i].startIndex > this._updatingBoundaries.endIndex ||
                this.spliceJunctions[i].endIndex < this._updatingBoundaries.startIndex) {
                continue;
            }
            _spliceJunctions.push(this.spliceJunctions[i]);
        }
        this.spliceJunctions = _spliceJunctions;
        _spliceJunctions = null;
    }

    _preprocessDownsampleCoverage() {
        let _downsampleItems = [];
        for (let i = 0; i < this.downsampleCoverage.length; i++) {
            if (this.downsampleCoverage[i].startIndex > this._updatingBoundaries.endIndex ||
                this.downsampleCoverage[i].endIndex < this._updatingBoundaries.startIndex) {
                continue;
            }
            _downsampleItems.push(this.downsampleCoverage[i]);
        }
        this.downsampleCoverage = _downsampleItems;
        _downsampleItems = null;
    }

    _preprocess(features) {
        if (!this._updatingBoundaries)
            return;
        if (features.alignments) {
            this._preprocessReads();
            this._preprocessDownsampleCoverage();
        }
        this._preprocessCoverage();
        this._preprocessRegions();
        this._preprocessSpliceJunctions();
    }

    rearrangeReads() {
        for (let g = 0; g < this._groups.length; g++) {
            if (this._groups[g].lines.length > 0)
                continue;
            this._groups[g].lines = [];
            let readsSorted = layoutReads(this._groups[g].rawReads);
            for (let i = 0; i < readsSorted.length; i++) {
                const read = readsSorted[i];
                while (this._groups[g].lines.length <= read.lineIndex)
                    this._groups[g].lines.push(new Line(this.track, this._groups[g].lines.length));
                this._groups[g].lines[read.lineIndex].push(read);
            }
            readsSorted = null;
        }
        this.prepareGroups();
    }

    preparePairedReads() {
        for (let g = 0; g < this._groups.length; g++) {
            let readNamesCache = new FastSet();
            const pairedReads = {};
            this._groups[g].rawPairedReads = [];
            let index = -1;
            for (let i = 0; i < this._groups[g].rawReads.length; i++) {
                const read = Object.assign({}, this._groups[g].rawReads[i]);
                if (!readNamesCache.has(read.name)) {
                    readNamesCache.add(read.name);
                    index++;
                    pairedReads[read.name] = index;
                    this._groups[g].rawPairedReads.push(read);
                } else {
                    actions.pair(this._groups[g].rawPairedReads[pairedReads[read.name]], read);
                }
            }
            for (let i = 0; i < this._groups[g].rawPairedReads.length; i++) {
                const read = this._groups[g].rawPairedReads[i];
                if (!read.isPairedReads) {
                    actions.fakePair(read, this.coverageRange);
                }
            }
            readNamesCache.clear();
            readNamesCache = null;
        }
    }

    rearrangeAsPairReads() {
        for (let g = 0; g < this._groups.length; g++) {
            if (this._groups[g].pairedLines.length > 0)
                continue;
            this._groups[g].pairedLines = [];
            const readsSorted = layoutReads(this._groups[g].rawPairedReads);
            for (let i = 0; i < readsSorted.length; i++) {
                const read = readsSorted[i];
                while (this._groups[g].pairedLines.length <= read.lineIndex)
                    this._groups[g].pairedLines.push(new Line(this.track, this._groups[g].pairedLines.length));
                this._groups[g].pairedLines[read.lineIndex].push(read);
            }
        }
        this.prepareGroups();
    }

    updateCoverageData(viewport, coverageTransformer, scaleConfig) {
        this.coverage.data = coverageTransformer.transform(this.coverageItems, viewport);
        this.coverage.coordinateSystem = coverageTransformer.transformCoordinateSystem(this.coverage.data, viewport, this.coverage.coordinateSystem, scaleConfig);
        this.coverage = Object.assign(this.coverage, {
            dataViewport: {
                endIndex: Math.round(Math.min(viewport.chromosomeSize, viewport.brush.end + viewport.brushSize / 2)),
                startIndex: Math.round(Math.max(1, viewport.brush.start - viewport.brushSize / 2))
            },
            isNew: true,
            viewport: {
                brush: {
                    end: viewport.brush.end,
                    start: viewport.brush.start
                },
                factor: viewport.factor
            }
        });
    }

    updateRegionsData() {
        this.regions.data = this.regionItems;
    }

    _appendDownsampleCoverage(data, cachePosition) {
        if (cachePosition === cachePositions.cachePositionMiddle) {
            this.downsampleCoverage = data.downsampleCoverage || [];
        } else {
            this.downsampleCoverage.push(...(data.downsampleCoverage || []));
        }
    }

    _appendCoverage(data, cachePosition) {
        if (!data.baseCoverage) {
            return;
        }
        if (cachePosition === cachePositions.cachePositionMiddle) {
            this.coverageItems = data.baseCoverage;
        } else {
            for (let i = 0; i < data.baseCoverage.length; i++) {
                this.coverageItems.push(data.baseCoverage[i]);
            }
        }
    }

    _appendRegions(data, cachePosition) {
        if (!data.regions) {
            return;
        }
        if (cachePosition === cachePositions.cachePositionMiddle) {
            this.regionItems = data.regions;
        } else {
            for (let i = 0; i < data.regions.length; i++) {
                this.regionItems.push(data.regions[i]);
            }
        }
    }

    _appendSpliceJunctions(data, cachePosition) {
        function addCoverage(data) {
            data.spliceJunctions.sort( (a,b) => {
                if (a.start < b.start) { return -1; }
                if (a.start === b.start && a.end <= b.end) { return -1; }
                return 1;
            });
            const auxiliarySet = new Set();
            data.spliceJunctions.map(item => {
                auxiliarySet.add(item.start);
                auxiliarySet.add(item.end);
            });
            const auxiliaryArray = Array.from(auxiliarySet)
                .sort((a,b) => a - b)
                .map((item, index, array) => {
                    if (index < array.length - 1) {
                        if (data.spliceJunctions.some(spliceJunction =>
                            spliceJunction.start <= item && spliceJunction.end >= array[index+1]
                        )) {
                            return {
                                start: item,
                                end: array[index + 1],
                            };
                        }
                    }
                    return undefined;})
                .filter(a => a);
            let n = 0, k = 0;
            while (k < (data.baseCoverage || []).length && n < auxiliaryArray.length) {
                const endIndex = data.baseCoverage[k].endIndex ?
                    data.baseCoverage[k].endIndex : data.baseCoverage[k].startIndex;
                if (
                    data.baseCoverage[k].startIndex >= auxiliaryArray[n].start &&
                    (endIndex <= auxiliaryArray[n].end)
                ) {
                    auxiliaryArray[n].coverage = Math.max(
                        data.baseCoverage[k].value,
                        (auxiliaryArray[n].coverage || 0)
                    );
                }
                if (endIndex >= auxiliaryArray[n].end) { n++; k--;}
                k++;
            }
            data.spliceJunctions.map(item => {
                let index = 0;
                const coverageArray = [];
                while (index < auxiliaryArray.length) {
                    if (item.start <= auxiliaryArray[index].start && item.end >= auxiliaryArray[index].end) {
                        coverageArray.push(auxiliaryArray[index].coverage);
                    }
                    if (item.end < auxiliaryArray[index].start) {
                        index = auxiliaryArray.length;
                    } else {
                        index++;
                    }
                }
                item.coverage = Math.max(...coverageArray);
            })
            return data.spliceJunctions;
        }
        data.spliceJunctions = data.spliceJunctions ? addCoverage(data) : [];
        if (cachePosition === cachePositions.cachePositionMiddle) {
            this.spliceJunctions = data.spliceJunctions;
        } else {
            for (let i = 0; i < data.spliceJunctions.length; i++) {

                let exists = false;
                for (let j = 0; j < this.spliceJunctions.length; j++) {
                    if (this.spliceJunctions[j].start === data.spliceJunctions[i].start &&
                        this.spliceJunctions[j].end === data.spliceJunctions[i].end &&
                        this.spliceJunctions[j].strand === data.spliceJunctions[i].strand) {
                        exists = true;
                        break;
                    }
                }
                if (!exists)
                    this.spliceJunctions.push(data.spliceJunctions[i]);
            }
        }
    }

    _appendReadToGroup(read, groupNames) {
        const {groupDisplayName, groupName} = actions.getGroupNamesFn(this.groupMode)(read);
        let index = groupNames.indexOf(groupName);
        if (index === -1) {
            this._groups.push({
                displayName: groupDisplayName,
                lines: [],
                name: groupName,
                pairedLines: [],
                rawPairedReads: [],
                rawReads: []
            });
            index = this._groups.length - 1;
            groupNames = this._groups.map(x => x.name);
        }
        this._groups[index].rawReads.push(read);
        return groupNames;
    }

    _appendReads(data, features) {
        if (features.alignments) {
            let groupNames = this._groups.map(x => x.name);
            if (!this.readNames) {
                this.readNames = new FastSet();
            }
            for (let i = 0; i < (data.blocks || []).length; i++) {
                const readName = `${data.blocks[i].name}-${data.blocks[i].startIndex}-${data.blocks[i].endIndex}-${data.blocks[i].tlen}`;
                if (this.readNames.has(readName)) {
                    continue;
                } else {
                    this.readNames.add(readName);
                }
                data.blocks[i].renderDump = transform(data.blocks[i], this.renderingProperties);
                groupNames = this._appendReadToGroup(data.blocks[i], groupNames);
            }
        }
        if (this.coverageItems) {
            for (let i = 0; i < this.coverageItems.length; i++) {
                if (this.coverageItems[i].endIndex === undefined) {
                    this.coverageItems[i].endIndex = this.coverageItems[i].startIndex;
                }
            }
            this.coverageItems = Sorting.quickSort(this.coverageItems, true, x => x.startIndex);
        }
        if (this.regionItems) {
            this.regionItems = Sorting.quickSort(this.regionItems, true, x => x.startIndex);
        }
        if (data.referenceBuffer) {
            this._referenceBuffers.push({
                endIndex: data.minPosition + data.referenceBuffer.length - 1,
                reference: data.referenceBuffer,
                startIndex: data.minPosition
            });
        }
    }

    _appendData(data, features, cachePosition = cachePositions.cachePositionMiddle): BamCache {
        if (!data) {
            return this;
        }
        if (this._delayedInvalidateRequested) {
            this.invalidate();
        }
        this._cacheUpdated = true;
        this._shouldRearrangeReads = true;
        this._appendCoverage(data, cachePosition);
        this._appendRegions(data, cachePosition);
        this._appendSpliceJunctions(data, cachePosition);
        if (features.alignments) {
            this._appendDownsampleCoverage(data, cachePosition);
        }
        this._appendReads(data, features);
        data = null;
        return this;
    }

    appendLeft(data, features): BamCache {
        return this._appendData(data, features, cachePositions.cachePositionLeft);
    }

    append(data, features): BamCache {
        return this._appendData(data, features);
    }

    appendRight(data, features): BamCache {
        return this._appendData(data, features, cachePositions.cachePositionRight);
    }

    prepareGroups() {
        let totalLines = 0;
        let totalPairedLines = 0;
        const firstInPairOrders = ['forward', 'reverse', '*'];
        const pairOrientationOrders = ['ll', 'rr', 'rl', 'lr', '*'];
        const criteria = (a, b) => {
            switch (this.groupMode) {
                case groupModes.groupByChromosomeOfMateMode: {
                    if (b.name === '*') {
                        return true;
                    }
                    else if (a.name === '*') {
                        return false;
                    }
                    else {
                        return b.name > a.name;
                    }
                }
                case groupModes.groupByFirstInPairMode:
                case groupModes.groupByReadStrandMode: {
                    return firstInPairOrders.indexOf(b.name) > firstInPairOrders.indexOf(a.name);
                }
                case groupModes.groupByPairOrientationMode: {
                    return pairOrientationOrders.indexOf(b.name) > pairOrientationOrders.indexOf(a.name);
                }
                default: return b.name > a.name;
            }
        };
        Sorting.quickSortWithComparison(this._groups, criteria);
        for (let g = 0; g < this._groups.length; g++) {
            const group = this._groups[g];
            group.previousLinesCount = totalLines;
            group.previousPairedLinesCount = totalPairedLines;
            totalLines += group.lines.length;
            totalPairedLines += group.pairedLines.length;
        }
        this._totalLines = totalLines;
        this._totalPairedLines = totalPairedLines;
    }

    group() {
        this._groups = actions.group(this._groups, this.groupMode);
        this.rearrange();
    }

    sort(mode, position) {
        actions.sort(this._groups, mode, position, this.renderingProperties.viewAsPairs);
    }

    clone() {
        if (this.isUpdating) {
            return undefined;
        }
        const cloneArray = source => (source || []).map(o => o);
        const cloneGroups = source => (source || []).map(group => ({
            displayName: group.groupDisplayName,
            lines: cloneArray(group.lines),
            name: group.groupName,
            pairedLines: cloneArray(group.pairedLines),
            rawPairedReads: cloneArray(group.rawPairedReads),
            rawReads: cloneArray(group.rawReads)
        }));
        const cloned = new BamCache(this.track, false);
        cloned.coverageRange = Object.assign({}, this.coverageRange);
        cloned.readNames = this.readNames.clone();
        cloned.downsampleCoverage = cloneArray(this.downsampleCoverage);
        cloned.coverageItems = cloneArray(this.coverageItems);
        cloned.regionItems = cloneArray(this.regionItems);
        cloned.spliceJunctions = cloneArray(this.spliceJunctions);
        cloned.coverage = Object.assign({}, this.coverage);
        cloned.regions = Object.assign({}, this.regions);
        cloned._groups = cloneGroups(this._groups);
        cloned._renderingProperties = Object.assign({}, this._renderingProperties);
        cloned._isUpdating = false;
        cloned._updatingBoundaries = null;
        cloned._shouldRearrangeReads = false;
        cloned._referenceBuffers = cloneArray(this._referenceBuffers);

        cloned._cacheUpdated = false;
        cloned._groupMode = this._groupMode;
        cloned._totalLines = this._totalLines;
        cloned._totalPairedLines = this._totalPairedLines;
        cloned._dataMode = this._dataMode;
        cloned._dataModeChanged = false;
        cloned._delayedInvalidateRequested = false;
        return cloned;
    }
}
