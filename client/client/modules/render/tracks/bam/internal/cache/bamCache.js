import * as actions from './actions';
import * as cachePositions from './bamCachePositions';
import {groupModes} from '../../modes';
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
    spliceJunctions = [];
    coverage = {coordinateSystem: null, data: null};
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

    get linesCount() {
        return this.renderingProperties.viewAsPairs ? this._totalPairedLines : this._totalLines;
    }

    get groups() {
        return this._groups;
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

    constructor(track) {
        this.track = track;
        this.invalidate();
    }

    invalidate(): BamCache {
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
        const needUpdateLeftSide = startIndex < this.coverageRange.startIndex - 1;
        const needUpdateRightSide = endIndex > this.coverageRange.endIndex + 1;
        const cacheIsComplete = startIndex >= this.coverageRange.startIndex && endIndex <= this.coverageRange.endIndex;
        const cacheIsInvalid = startIndex > this.coverageRange.endIndex || endIndex < this.coverageRange.startIndex;
        return {cacheIsComplete, cacheIsInvalid, needUpdateLeftSide, needUpdateRightSide};
    }

    startUpdate(boundaries): BamCache {
        if (!this._isUpdating) {
            this._updatingBoundaries = boundaries;
        }
        this._isUpdating = true;
        this._cacheUpdated = false;
        return this;
    }

    endUpdate(viewport, coverageTransformer) {
        if (this._updatingBoundaries) {
            this.coverageRange.startIndex = this._updatingBoundaries.startIndex;
            this.coverageRange.endIndex = this._updatingBoundaries.endIndex;
            this._preprocess();
            this._updatingBoundaries = null;
        }
        if (this._shouldRearrangeReads) {
            this.rearrange();
        }
        this._shouldRearrangeReads = false;
        this.updateCoverageData(viewport, coverageTransformer);
        this._isUpdating = false;
        return this._cacheUpdated;
    }

    rearrange() {
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
        for (let i = 0; i < this.coverageItems.length; i++) {
            if (this.coverageItems[i].startIndex > this._updatingBoundaries.endIndex ||
                this.coverageItems[i].endIndex < this._updatingBoundaries.startIndex) {
                continue;
            }
            _coverageItems.push(this.coverageItems[i]);
        }
        this.coverageItems = _coverageItems;
        _coverageItems = null;
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

    _preprocess() {
        if (!this._updatingBoundaries)
            return;
        this._preprocessReads();
        this._preprocessCoverage();
        this._preprocessSpliceJunctions();
        this._preprocessDownsampleCoverage();
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

    updateCoverageData(viewport, coverageTransformer) {
        this.coverage.data = coverageTransformer.transform(this.coverageItems, viewport);
        this.coverage.coordinateSystem = coverageTransformer.transformCoordinateSystem(this.coverage.data, viewport, this.coverage.coordinateSystem);
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

    _appendDownsampleCoverage(data, cachePosition) {
        if (cachePosition === cachePositions.cachePositionMiddle) {
            this.downsampleCoverage = data.downsampleCoverage || [];
        } else {
            this.downsampleCoverage.push(...(data.downsampleCoverage || []));
        }
    }

    _appendCoverage(data, cachePosition) {
        if (cachePosition === cachePositions.cachePositionMiddle) {
            this.coverageItems = data.baseCoverage;
        } else {
            for (let i = 0; i < data.baseCoverage.length; i++) {
                this.coverageItems.push(data.baseCoverage[i]);
            }
        }
    }

    _appendSpliceJunctions(data, cachePosition) {
        if (cachePosition === cachePositions.cachePositionMiddle) {
            this.spliceJunctions = data.spliceJunctions || [];
        } else {
            for (let i = 0; i < (data.spliceJunctions || []).length; i++) {

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

    _appendReads(data) {
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
        for (let i = 0; i < this.coverageItems.length; i++) {
            this.coverageItems[i].endIndex = this.coverageItems[i].startIndex;
        }
        this.coverageItems = Sorting.quickSort(this.coverageItems, true, x => x.startIndex);
        if (data.referenceBuffer) {
            this._referenceBuffers.push({
                endIndex: data.minPosition + data.referenceBuffer.length,
                reference: data.referenceBuffer,
                startIndex: data.minPosition
            });
        }
    }

    _appendData(data, cachePosition = cachePositions.cachePositionMiddle): BamCache {
        if (!data || !data.blocks || data.blocks.length === 0) {
            return this;
        }
        this._cacheUpdated = true;
        this._shouldRearrangeReads = true;
        this._appendCoverage(data, cachePosition);
        this._appendDownsampleCoverage(data, cachePosition);
        this._appendSpliceJunctions(data, cachePosition);
        this._appendReads(data);
        data = null;
        return this;
    }

    appendLeft(data): BamCache {
        return this._appendData(data, cachePositions.cachePositionLeft);
    }

    append(data): BamCache {
        return this._appendData(data);
    }

    appendRight(data): BamCache {
        return this._appendData(data, cachePositions.cachePositionRight);
    }

    prepareGroups() {
        let totalLines = 0;
        let totalPairedLines = 0;
        const firstInPairOrders = ['l', 'r', '*'];
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
}