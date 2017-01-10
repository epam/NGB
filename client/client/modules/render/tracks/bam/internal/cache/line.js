import * as partTypes from '../../modes/partTypes';
import {undumpInfo, undumpPart} from '../internalUtilities';
import Dict from 'collections/dict';
import SortedSet from 'collections/sorted-set';

Dict.prototype.assertString = function (key) {
    if (typeof key !== 'string' && typeof key !== 'number') {
        throw new TypeError(`key must be a string but got ${key}`);
    }
};


export class Line {
    itemsSortedByStartIndex = new SortedSet([], undefined, (a, b) => a.startIndex - b.startIndex);
    brush = {end: 0, start: 0};
    sortData = {};
    index: number;
    initialIndex: number;
    range = {end: -Infinity, start: Infinity};
    itemsCount: number = 0;

    constructor(track: Track, index: number) {
        this.track = track;
        this.index = index;
        this.initialIndex = index;
    }

    static getIntersectionRanges(render) {
        let connectionRange = {
            endIndex: render.rightPair.startIndex,
            startIndex: render.leftPair.endIndex + 1
        };
        let commonRange = null;
        if (connectionRange.endIndex < connectionRange.startIndex) {
            commonRange = {
                endIndex: connectionRange.startIndex + 2,
                startIndex: connectionRange.endIndex - 2
            };
            connectionRange = null;
        }
        return {
            commonRange,
            connectionRange
        };
    }

    render(start, end, {arrows, diffBase, ins_del, softClip}) {
        let cursor = start;
        let pointer = this.itemsSortedByStartIndex.findGreatestLessThanOrEqual({startIndex: cursor});
        if (!pointer || pointer.value.endIndex < start)
            pointer = this.itemsSortedByStartIndex.findLeastGreaterThan({startIndex: cursor});
        let result = [];
        let endOfLineOrNoReadFound = !pointer;
        while (!endOfLineOrNoReadFound) {
            const render = pointer.value;

            if (render.isPairedReads) {
                const {commonRange, connectionRange} = Line.getIntersectionRanges(render);
                result = result.concat(this.getReadRenderers(render.leftPair, {
                    arrows,
                    diffBase,
                    ins_del,
                    softClip
                }, {isLeftPair: true, isRightPair: false}, commonRange));
                result = result.concat(this.getReadRenderers(render.rightPair, {
                    arrows,
                    diffBase,
                    ins_del,
                    softClip
                }, {isLeftPair: false, isRightPair: true}, commonRange));
                if (connectionRange) {
                    result = result.concat([{
                        endIndex: connectionRange.endIndex,
                        startIndex: connectionRange.startIndex,
                        type: partTypes.pairedReadConnection
                    }]);
                }
            }
            else {
                result = result.concat(this.getReadRenderers(render, {
                    arrows,
                    diffBase,
                    ins_del,
                    softClip
                }, null, null));
            }

            cursor = pointer.value.endIndex;

            pointer = this.itemsSortedByStartIndex.findLeastGreaterThan({startIndex: cursor});
            if (cursor > end || !pointer || !pointer.value || pointer.value.startIndex > end) {
                endOfLineOrNoReadFound = true;
            }
        }
        return result;
    }

    _splitRenderers(renderers, commonRange, isLeftPair, isRightPair) {
        let result = [];
        for (let i = 0; i < renderers.length; i++) {
            renderers[i].isPaired = isLeftPair || isRightPair;
            renderers[i].isOverlaps = false;
            renderers[i].isLeft = isLeftPair;
            renderers[i].isRight = isRightPair;
            if (commonRange) {
                if (renderers[i].startIndex >= commonRange.startIndex && renderers[i].endIndex <= commonRange.endIndex) {
                    renderers[i].isOverlaps = true;
                    result.push(renderers[i]);
                }
                else if (renderers[i].startIndex > commonRange.endIndex || renderers[i].endIndex < commonRange.startIndex) {
                    renderers[i].isOverlaps = false;
                    result.push(renderers[i]);
                }
                else {
                    // we should split renderer
                    if (renderers[i].length <= 1) {
                        result.push(renderers[i]);
                    }
                    else
                    if (renderers[i].endIndex > commonRange.startIndex && renderers[i].endIndex <= commonRange.endIndex) {
                        const lPart = Object.assign({}, renderers[i]);
                        const rPart = Object.assign({}, renderers[i]);
                        lPart.endIndex = commonRange.startIndex;
                        lPart.length = lPart.endIndex - lPart.startIndex;
                        rPart.startIndex = commonRange.startIndex;// + 1;
                        rPart.length = rPart.endIndex - rPart.startIndex;
                        lPart.isOverlaps = false;
                        rPart.isOverlaps = true;
                        result.push(lPart);
                        result.push(rPart);
                    }
                    else if (renderers[i].startIndex > commonRange.startIndex && renderers[i].startIndex <= commonRange.endIndex) {
                        const lPart = Object.assign({}, renderers[i]);
                        const rPart = Object.assign({}, renderers[i]);
                        lPart.endIndex = commonRange.endIndex;
                        lPart.length = lPart.endIndex - lPart.startIndex;
                        rPart.startIndex = commonRange.endIndex;// + 1;
                        rPart.length = rPart.endIndex - rPart.startIndex;
                        lPart.isOverlaps = true;
                        rPart.isOverlaps = false;
                        result.push(lPart);
                        result.push(rPart);
                    }
                    else {
                        const lPart = Object.assign({}, renderers[i]);
                        const mPart = Object.assign({}, renderers[i]);
                        const rPart = Object.assign({}, renderers[i]);
                        lPart.endIndex = commonRange.startIndex;
                        lPart.length = lPart.endIndex - lPart.startIndex;
                        mPart.startIndex = commonRange.startIndex;// + 1;
                        mPart.endIndex = commonRange.endIndex;
                        mPart.length = mPart.endIndex - mPart.startIndex;
                        rPart.startIndex = commonRange.endIndex;// + 1;
                        rPart.length = rPart.endIndex - rPart.startIndex;
                        lPart.isOverlaps = false;
                        mPart.isOverlaps = true;
                        rPart.isOverlaps = false;
                        result.push(lPart);
                        result.push(mPart);
                        result.push(rPart);
                    }
                }
            }
            else {
                result.push(renderers[i]);
            }
        }
        result = result.filter(x => x.type === partTypes.leftArrow || x.type === partTypes.rightArrow || x.length > 0);
        return result;
    }

    static concatRenderers(currentRenderer, {arrows, diffBase, ins_del, softClip}, isLeftPair, isRightPair) {
        let renderers = [];
        renderers = renderers.concat([
            {
                isPaired: isLeftPair || isRightPair,
                spec: currentRenderer.spec,
                type: partTypes.initRead
            }
        ]);

        renderers = renderers.concat(currentRenderer.minimal);
        if (arrows && currentRenderer.arrows)
            renderers = renderers.concat(currentRenderer.arrows);
        if (diffBase && currentRenderer.diffBase)
            renderers = renderers.concat(currentRenderer.diffBase);
        if (ins_del && currentRenderer.ncigar)
            renderers = renderers.concat(currentRenderer.ncigar);
        if (softClip && currentRenderer.softClip)
            renderers = renderers.concat(currentRenderer.softClip);

        return renderers;
    }

    getReadRenderers(read, {arrows, diffBase, ins_del, softClip}, pairInfo = null, commonRange = null) {
        if (read.isUnknown)
            return [];
        if (!pairInfo) {
            pairInfo = {
                isLeftPair: false,
                isRightPair: false
            };
        }

        const {isLeftPair, isRightPair} = pairInfo;

        let __render = null;

        if (!read.render) {
            read.render = {
                arrows: read.renderDump.arrows.map(undumpPart),
                diffBase: read.renderDump.diffBase.map(undumpPart),
                info: undumpInfo(read.renderDump.info),
                minimal: read.renderDump.minimal.map(undumpPart),
                ncigar: read.renderDump.ncigar.map(undumpPart),
                softClip: read.renderDump.softClip.map(undumpPart),
                spec: read.renderDump.spec
            };
        }

        if (isLeftPair || isRightPair) {
            if (!read.pairedRender) {
                read.pairedRender = {
                    arrows: this._splitRenderers(read.renderDump.arrows.map(undumpPart), commonRange, isLeftPair, isRightPair),
                    diffBase: this._splitRenderers(read.renderDump.diffBase.map(undumpPart), commonRange, isLeftPair, isRightPair),
                    info: undumpInfo(read.renderDump.info),
                    minimal: this._splitRenderers(read.renderDump.minimal.map(undumpPart), commonRange, isLeftPair, isRightPair),
                    ncigar: this._splitRenderers(read.renderDump.ncigar.map(undumpPart), commonRange, isLeftPair, isRightPair),
                    softClip: this._splitRenderers(read.renderDump.softClip.map(undumpPart), commonRange, isLeftPair, isRightPair),
                    spec: read.renderDump.spec
                };
            }
            __render = read.pairedRender;
        }
        else {
            __render = read.render;
        }

        return Line.concatRenderers(__render, {arrows, diffBase, ins_del, softClip}, isLeftPair, isRightPair);
    }

    push(entry) {
        this.itemsCount++;
        if (entry.startIndex < this.range.start) {
            this.range.start = entry.startIndex;
        }
        if (entry.endIndex > this.range.end) {
            this.range.end = entry.endIndex;
        }
        this.itemsSortedByStartIndex.add(entry);
    }

    replace(entry) {
        this.itemsSortedByStartIndex.delete(this.itemsSortedByStartIndex.find(entry).value);
        this.push(entry);
    }

    findEntries(criteria) {
        return this.itemsSortedByStartIndex.filter(criteria);
    }

    setNewIndex(index) {
        this.itemsSortedByStartIndex.forEach(item => item.lineIndex = index);
    }
}