import * as ViewportTransformTypes from './viewportTransofrmTypes';
import {GeneDataService} from '../../../../../dataServices/gene/gene-data-service';

const Math = window.Math;

class ShortenedIntronsBrush {
    startIndex: number = null;
    endIndex: number = null;
    center: number = null;
    shortenedSize: number = null;

    relativeStartIndex = null;

    constructor() {

    }
}

export default class ShortenedIntronsViewport {

    brush: ShortenedIntronsBrush = null;

    _geneDataService = new GeneDataService();

    _coveredRange = null;
    _intronLength = 0;
    _maximumRange = 0;
    _shortenedIntronsMode = false;
    _configuration = null;

    _rebuildRequested = false;

    _viewport;

    constructor(viewport) {
        this._viewport = viewport;
    }

    get ranges() {
        if (this._coveredRange) {
            return this._coveredRange.ranges;
        }
        return [];
    }

    get shortenedIntronsMode() {
        return this._shortenedIntronsMode;
    }

    get shortenedIntronsTrackId() {
        return this._configuration ? this._configuration.id : null;
    }

    get intronLength() {
        return this._intronLength;
    }

    set intronLength(value) {
        if (value !== undefined && value !== null && this._intronLength !== value) {
            this._intronLength = value;
            this.invalidate();
        }
    }

    get maximumRange() {
        return this._maximumRange;
    }

    set maximumRange(value) {
        if (value !== undefined && value !== null) {
            this._maximumRange = value;
        }
    }

    disable() {
        this.enable(null);
        this.brush = null;
    }

    enable(track) {
        if (track && this._configuration &&
            this._configuration.id === track.config.id &&
            this._configuration.chromosomeId === track.config.chromosomeId)
            return;
        this._configuration = track ? track.config : null;
        this._shortenedIntronsMode = this._configuration !== null;
        this.invalidate();
    }

    invalidate() {
        this._viewport.shortenedIntronsChangeSubject.onNext();
        if (this._shortenedIntronsMode) {
            this._rebuildRequested = true;
            this._viewport.transform({});
        } else {
            this._coveredRange = null;
            if (this.brush) {
                this._viewport.transform({
                    end: this.brush.center + this.brush.shortenedSize / 2,
                    start: this.brush.center - this.brush.shortenedSize / 2,
                    awakeFromShortenedIntrons: true
                });
            }
        }
    }

    getTransformType({start, end, delta}) {
        if (this.brush) {
            if (this._rebuildRequested) {
                this._rebuildRequested = false;
                return ViewportTransformTypes.VIEWPORT_TRANSFORM_REBUILD;
            }
            if (delta) {
                const newCenter = this.translatePosition(this.brush.center, delta);
                const newStart = this.translatePosition(newCenter, -this.brush.shortenedSize / 2);
                const newEnd = this.translatePosition(newCenter, +this.brush.shortenedSize / 2);
                if (this.checkCoverage(newStart) && this.checkCoverage(newEnd)) {
                    return ViewportTransformTypes.VIEWPORT_MOVE;
                }
            }
            if (this.brush.center &&
                this.brush.shortenedSize &&
                start &&
                end &&
                Math.round((start + end) / 2) === Math.round((this.brush.startIndex + this.brush.endIndex) / 2)) {
                return ViewportTransformTypes.VIEWPORT_SCALE;
            }
            if (this.brush.startIndex &&
                this.brush.endIndex &&
                start &&
                end &&
                this.brush.startIndex <= start && this.brush.endIndex >= end) {
                return ViewportTransformTypes.VIEWPORT_TRANSFORM_LOCAL;
            }
            if (this.brush.startIndex &&
                this.brush.endIndex &&
                start &&
                end &&
                this.brush.startIndex >= start && this.brush.endIndex <= end) {
                return ViewportTransformTypes.VIEWPORT_TRANSFORM_GLOBAL;
            }
        }
        return ViewportTransformTypes.VIEWPORT_TRANSFORM_SET;
    }

    checkCoverage(position, delta = 0) {
        if (delta) {
            position = this.translatePosition(position, delta);
        }
        if (position) {
            return position >= this._coveredRange.startIndex && position <= this._coveredRange.endIndex;
        }
        return false;
    }

    checkFeature({startIndex, endIndex}) {
        if (!this._coveredRange)
            return true;
        if (startIndex === undefined)
            return false;
        if (endIndex === undefined) {
            endIndex = startIndex;
        }
        if (endIndex < this._coveredRange.startIndex || startIndex > this._coveredRange.endIndex)
            return false;
        for (let i = 0; i < this._coveredRange.ranges.length; i++) {
            const range = this._coveredRange.ranges[i];
            if (startIndex <= range.endIndex && endIndex >= range.startIndex)
                return true;
        }
        return false;
    }

    translateStartPosition(delta) {
        if (this.brush) {
            const newRelativePosition = this.brush.relativeStartIndex + delta;
            let matchedRange = null;
            for (let i = 0; i < this._coveredRange.ranges.length; i++) {
                if (this._coveredRange.ranges[i].relative.startIndex <= newRelativePosition &&
                    this._coveredRange.ranges[i].relative.endIndex >= newRelativePosition) {
                    matchedRange = this._coveredRange.ranges[i];
                    break;
                }
            }
            if (matchedRange) {
                const deltaFromStart = newRelativePosition - matchedRange.relative.startIndex;
                return matchedRange.startIndex + deltaFromStart;
            }
            else {
                if (newRelativePosition <= this._coveredRange.relative.startIndex) {
                    const delta = this._coveredRange.relative.startIndex - newRelativePosition;
                    return this._coveredRange.startIndex - delta;
                }
                else {
                    const delta = newRelativePosition - this._coveredRange.relative.endIndex;
                    return this._coveredRange.endIndex + delta;
                }
            }
        }
        return null;
    }

    translatePosition(position, delta) {
        const relativePosition = this.getShortenedRelativePosition(position);
        if (relativePosition !== null) {
            const newRelativePosition = relativePosition + delta;
            let matchedRange = null;
            for (let i = 0; i < this._coveredRange.ranges.length; i++) {
                if (this._coveredRange.ranges[i].relative.startIndex <= newRelativePosition &&
                    this._coveredRange.ranges[i].relative.endIndex >= newRelativePosition) {
                    matchedRange = this._coveredRange.ranges[i];
                    break;
                }
            }
            if (matchedRange) {
                const deltaFromStart = newRelativePosition - matchedRange.relative.startIndex;
                return matchedRange.startIndex + deltaFromStart;
            }
            else {
                if (newRelativePosition <= this._coveredRange.relative.startIndex) {
                    const delta = this._coveredRange.relative.startIndex - newRelativePosition;
                    return this._coveredRange.startIndex - delta;
                }
                else {
                    const delta = newRelativePosition - this._coveredRange.relative.endIndex;
                    return this._coveredRange.endIndex + delta;
                }
            }
        }
        return null;
    }

    getShortenedSize({start, end}) {
        const startInfo = this.getShortenedPositionInfo(start);
        const endInfo = this.getShortenedPositionInfo(end);
        if (startInfo && endInfo) {
            return endInfo.relativePosition - startInfo.relativePosition + 1;
        }
        return null;
    }

    getShortenedPositionInfo(position) {
        for (let i = 0; i < this._coveredRange.ranges.length; i++) {
            const range = this._coveredRange.ranges[i];
            if (range.soft.startIndex <= position && range.soft.endIndex >= position) {
                let pos = Math.max(Math.min(range.endIndex, position), range.startIndex);
                if (position < range.startIndex) {
                    pos--;
                }
                if (position > range.endIndex) {
                    pos++;
                }
                const relativePosition = range.relative.startIndex + (pos - range.startIndex);
                return {
                    position: pos,
                    rangeIndex: i,
                    relativePosition: relativePosition
                };
            }
        }
        return null;
    }

    getShortenedPosition(position) {
        const info = this.getShortenedPositionInfo(position);
        if (info) {
            return info.position;
        }
        return null;
    }

    getShortenedRelativePosition(position) {
        const info = this.getShortenedPositionInfo(position);
        if (info) {
            return info.relativePosition;
        }
        return null;
    }

    async rebuildRanges({center, brushSize, start, end}) {
        let blocks = [];
        if (center !== undefined && brushSize !== undefined) {
            blocks = await this._geneDataService.getExonsByViewport({
                centerPosition: center,
                chromosomeId: this._configuration.chromosomeId,
                id: this._configuration.id,
                projectId: this._configuration.projectIdNumber || undefined,
                intronLength: this.intronLength,
                viewPortSize: 2 * brushSize
            });
        } else if (start !== undefined && end !== undefined) {
            blocks = await this._geneDataService.getExonsByRange({
                chromosomeId: this._configuration.chromosomeId,
                endIndex: Math.min(this._viewport.chromosome.end, end + (end - start) / 2),
                id: this._configuration.id,
                projectId: this._configuration.projectIdNumber || undefined,
                intronLength: this.intronLength,
                startIndex: Math.max(this._viewport.chromosome.start, start - (end - start) / 2),
            });
        }
        if (blocks.length > 0) {
            this._coveredRange = {
                endIndex: blocks[blocks.length - 1].endIndex,
                ranges: blocks,
                relative: {
                    endIndex: null,
                    startIndex: 1
                },
                startIndex: blocks[0].startIndex
            };
            let relativePosition = this._coveredRange.relative.startIndex;
            let prevSoftIndex = this._viewport.chromosome.start;
            for (let i = 0; i < this._coveredRange.ranges.length; i++) {
                const length = (this._coveredRange.ranges[i].endIndex - this._coveredRange.ranges[i].startIndex + 1);
                this._coveredRange.ranges[i].relative = {
                    endIndex: relativePosition + length - 1,
                    startIndex: relativePosition
                };
                let nextSoftIndex = this._viewport.chromosome.end;
                if (i < this._coveredRange.ranges.length - 1) {
                    nextSoftIndex = Math.round((this._coveredRange.ranges[i + 1].startIndex + this._coveredRange.ranges[i].endIndex) / 2);
                }
                this._coveredRange.ranges[i].soft = {
                    endIndex: nextSoftIndex,
                    startIndex: prevSoftIndex
                };
                prevSoftIndex = nextSoftIndex;
                relativePosition += length;
            }
            this._coveredRange.relative.endIndex = relativePosition;
        }
    }

    async _transformViewportSet(start, end) {
        await this.rebuildRanges({
            brushSize: (end - start),
            center: (start + end) / 2,
        });
        this.brush = new ShortenedIntronsBrush();
        this.brush.shortenedSize = (end - start + 1);
        this.brush.center = this.getShortenedPosition((start + end) / 2);
        this.brush.startIndex = this.translatePosition(this.brush.center, -this.brush.shortenedSize / 2);
        this.brush.endIndex = this.translatePosition(this.brush.center, +this.brush.shortenedSize / 2);
        this.brush.relativeStartIndex = this.getShortenedRelativePosition(this.brush.startIndex);
    }

    async _transformViewportMove(delta) {
        this.brush.center = this.translatePosition(this.brush.center, delta);
        this.brush.startIndex = this.translatePosition(this.brush.center, -this.brush.shortenedSize / 2);
        this.brush.endIndex = this.translatePosition(this.brush.center, +this.brush.shortenedSize / 2);
        this.brush.relativeStartIndex = this.getShortenedRelativePosition(this.brush.startIndex);
    }

    async _transformViewportScale(factor) {
        this.brush.shortenedSize *= factor;
        const newStart = this.translatePosition(this.brush.center, -this.brush.shortenedSize / 2);
        const newEnd = this.translatePosition(this.brush.center, +this.brush.shortenedSize / 2);
        if (!this.checkCoverage(newStart) || !this.checkCoverage(newEnd)) {
            await this.rebuildRanges({
                brushSize: this.brush.shortenedSize,
                center: this.brush.center
            });
        }
        this.brush.startIndex = this.translatePosition(this.brush.center, -this.brush.shortenedSize / 2);
        this.brush.endIndex = this.translatePosition(this.brush.center, +this.brush.shortenedSize / 2);
        this.brush.relativeStartIndex = this.getShortenedRelativePosition(this.brush.startIndex);
    }

    async _transformViewportGlobal(start, end) {
        if (!this.checkCoverage(start) || !this.checkCoverage(end)) {
            const newStart = Math.max(this._viewport.chromosome.start, start - (end - start) / 2);
            const newEnd = Math.min(this._viewport.chromosome.end, end + (end - start) / 2);
            await this.rebuildRanges({end: newEnd, start: newStart});
        }
        this.brush.shortenedSize = this.getShortenedSize({end, start});
        this.brush.startIndex = start;
        this.brush.endIndex = end;
        this.brush.center = this.translatePosition(start, this.brush.shortenedSize / 2);
        this.brush.relativeStartIndex = this.getShortenedRelativePosition(this.brush.startIndex);
    }

    async _transformViewportLocal(start, end) {
        this.brush.startIndex = start;
        this.brush.endIndex = end;
        this.brush.shortenedSize = this.getShortenedSize({end, start});
        this.brush.relativeStartIndex = this.getShortenedRelativePosition(this.brush.startIndex);
    }

    async _transformViewportRebuild() {
        await this.rebuildRanges({
            brushSize: this.brush.shortenedSize,
            center: this.brush.center
        });
        this.brush.startIndex = this.translatePosition(this.brush.center, -this.brush.shortenedSize / 2);
        this.brush.endIndex = this.translatePosition(this.brush.center, +this.brush.shortenedSize / 2);
        this.brush.relativeStartIndex = this.getShortenedRelativePosition(this.brush.startIndex);
    }

    async transform(parameters) {
        const {start, end, delta} = parameters;
        const transformType = this.getTransformType(parameters);
        switch (transformType) {
            case ViewportTransformTypes.VIEWPORT_TRANSFORM_SET: {
                await this._transformViewportSet(start || this._viewport.brush.start, end || this._viewport.brush.end);
            }
                break;
            case ViewportTransformTypes.VIEWPORT_MOVE: {
                await this._transformViewportMove(delta);
            }
                break;
            case ViewportTransformTypes.VIEWPORT_SCALE: {
                await this._transformViewportScale(((end - start) / (this.brush.endIndex - this.brush.startIndex)));
            }
                break;
            case ViewportTransformTypes.VIEWPORT_TRANSFORM_GLOBAL: {
                await this._transformViewportGlobal(start, end);
            }
                break;
            case ViewportTransformTypes.VIEWPORT_TRANSFORM_LOCAL: {
                await this._transformViewportLocal(start, end);
            }
                break;
            case ViewportTransformTypes.VIEWPORT_TRANSFORM_REBUILD: {
                await this._transformViewportRebuild();
            }
                break;
        }

        if (this.brush) {
            this._viewport.brush.start = this.brush.startIndex;
            this._viewport.brush.end = this.brush.endIndex;
        }
    }

    transformFeature(feature) {
        if (this._coveredRange && this._coveredRange.ranges) {
            for (let i = 0; i < this._coveredRange.ranges.length; i++) {
                const range = this._coveredRange.ranges[i];
                if ((feature.startIndex >= range.startIndex && feature.startIndex <= range.endIndex) ||
                    (feature.endIndex >= range.startIndex && feature.endIndex <= range.endIndex) ||
                    (feature.startIndex <= range.startIndex && feature.endIndex >= range.endIndex)) {
                    feature.startIndex = Math.max(feature.startIndex, range.startIndex);
                    feature.endIndex = Math.min(feature.endIndex, range.endIndex);
                    return feature;
                }
            }
        }
        return null;
    }

    shouldSkipFeature(feature) {
        let matchedRange = null;
        if (this._coveredRange && this._coveredRange.ranges) {
            for (let i = 0; i < this._coveredRange.ranges.length; i++) {
                const range = this._coveredRange.ranges[i];
                if ((feature.startIndex >= range.startIndex && feature.startIndex <= range.endIndex) ||
                    (feature.endIndex >= range.startIndex && feature.endIndex <= range.endIndex) ||
                    (feature.startIndex <= range.startIndex && feature.endIndex >= range.endIndex)) {
                    matchedRange = range;
                    break;
                }
            }
        }
        return !matchedRange;
    }

    transformFeaturesArray(features) {
        return features.map(feature => this.transformFeature(feature)).filter(feature => feature !== null);
    }
}
