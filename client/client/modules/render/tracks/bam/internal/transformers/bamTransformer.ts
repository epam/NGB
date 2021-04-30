import * as partTypes from '../../modes/partTypes';
import {dumpInfo, dumpPart} from '../internalUtilities';

interface Part {
    type: Symbol,
    startIndex: number,
    length: number
}

interface ServerBlock {
    startIndex: number;
    endIndex: number;
    mappingQuality: number;
    name: string;
    differentBase?: Array<{base: string, relativePosition: number}>;
    cigarString: string,
    headSequence?: string,
    tailSequence?: string,
    ncigar?: Array<{
        /*
         * i - insertion
         * d - deletion
         * m - match (everything OK)
         * s - soft-clip
         * n - splice junction
         */
        length: number,
        string: 'I' | 'D' | 'M' | 'S' | 'N'
    }>;
    rnext: string;
    stand: boolean;
    tlen: number;
    flags?: Array<'READ_REVERSE_STRAND'>,
    flagMask: number;
    pnext: number;
    rname: string;
}

const mappingThreshold = 15;
const ncigarRegex = /(\d*)([MIDNSHP=X])/img;

function parseNcigar(string) {
    let value = ncigarRegex.exec(string);
    const result = [];
    while (value) {
        result.push({
            length: parseInt(value[1]),
            string: value[2]
        });
        value = ncigarRegex.exec(string);
    }
    return result;
}

function createMinimalRender(block: ServerBlock, ncigar, isSoftClipping = false): Part[] {
    const part:Part = {
        length: 0,
        startIndex: block.startIndex,
        type: partTypes.match
    };
    for (let i = 0; i < ncigar.length; i++) {
        const cigar = ncigar[i];
        switch (cigar.string) {
            case 'D':
            case 'M':
            case 'N': {
                part.length += cigar.length;
            }
                break;
            case 'S': {
                if (isSoftClipping) {
                    part.length += cigar.length;
                }
            }
                break;
        }
    }

    return [part];
}

function createNcigarRender(block: ServerBlock, ncigar, isSoftClipping = false): Part[] {
    let cursor = block.startIndex;
    const parts:Part[] = [];
    for (let i = 0; i < ncigar.length; i++) {
        const cigar = ncigar[i];
        switch (cigar.string) {
            case 'S': {
                parts.push({
                    length: cigar.length,
                    startIndex: cursor,
                    type: partTypes.softClip
                });
                if (isSoftClipping)
                    cursor += cigar.length;
            }
                break;
            case 'I': {
                parts.push({
                    length: cigar.length,
                    startIndex: cursor,
                    type: partTypes.insertion
                });
            }
                break;
            case 'D': {
                parts.push({
                    length: cigar.length,
                    startIndex: cursor,
                    type: partTypes.deletion
                });
                cursor += cigar.length;
            }
                break;
            case 'N': {
                parts.push({
                    length: cigar.length,
                    startIndex: cursor,
                    type: partTypes.spliceJunctions
                });
                cursor += cigar.length;
            }
                break;
            case 'M': {
                cursor += cigar.length;
            }
                break;
        }
    }
    return parts;
}

function createDiffBaseRender(block: ServerBlock): Part[] {
    if (!block.differentBase)
        return [];
    return block.differentBase.map(({base, relativePosition}) => ({
        base: base,
        length: 1,
        startIndex: block.startIndex + relativePosition,
        type: partTypes.base
    }));
}

function createSoftClipRender(block: ServerBlock): Part[] {
    const render = [];
    if (block.headSequence) {
        render.push(..._createSoftClipBaseRender(block.headSequence, block.startIndex));
    }
    if (block.tailSequence) {
        render.push(..._createSoftClipBaseRender(block.tailSequence, block.endIndex - block.tailSequence.length + 1));
    }
    return render;
}

function _createSoftClipBaseRender(sequence, position): Part[] {
    if (!sequence)
        return [];
    const parts = [];
    for (let i = 0; i < sequence.length; i++) {
        parts.push({
            base: sequence[i],
            length: 1,
            startIndex: position + i,
            type: partTypes.softClipBase
        });
    }
    return parts;
}

function testBit(hexMask, bit) {
    return !!(hexMask & bit);
}

const READ_PAIRED_FLAG = 0x1;
const READ_UNMAPPED_FLAG = 0x4;
const READ_MATE_UNPAIRED_FLAG = 0x8;
const READ_REVERSE_STRAND_FLAG = 0x10;
const READ_MATE_REVERSE_STRAND_FLAG = 0x20;
const READ_FIRST_IN_PAIR_FLAG = 0x40;

function getPairOrientation(block: ServerBlock, simplified = false) {
    const hexMask = block.flagMask;
    const isPairedAndMapped =
        (hexMask & READ_PAIRED_FLAG) !== 0
        && (hexMask & READ_UNMAPPED_FLAG) === 0 //Is mapped
        && (hexMask & READ_MATE_UNPAIRED_FLAG) === 0; //Is mate mapped
    if (!isPairedAndMapped) {
        return 'NA'; // read not paired
    }
    const self = (testBit(hexMask, READ_REVERSE_STRAND_FLAG) ? 'R' : 'L') +
        (simplified ? '' : (testBit(hexMask, READ_FIRST_IN_PAIR_FLAG) ? '1' : '2'));
    const mate = (testBit(hexMask, READ_MATE_REVERSE_STRAND_FLAG) ? 'R' : 'L') +
        (simplified ? '' : (testBit(hexMask, READ_FIRST_IN_PAIR_FLAG) ? '2' : '1'));
    if (block.startIndex < block.pnext) {
        return self + mate;
    }
    else {
        return mate + self;
    }
}

function getInsertSizeType(block, renderProps) {
    const isPairedAndMapped =
        (block.flagMask & READ_PAIRED_FLAG) !== 0
        && (block.flagMask & READ_UNMAPPED_FLAG) === 0 //Is mapped
        && (block.flagMask & READ_MATE_UNPAIRED_FLAG) === 0; //Is mate mapped

    if (!isPairedAndMapped) return 'happy';

    const len = Math.abs(block.tlen);
    if (block.rnext !== '*'
        && block.rnext !== '='
        && block.rnext.toUpperCase() !== block.rname.toUpperCase()
    ) {
        return 'otherChr';
    }
    if (len > renderProps.maxBpCount) {
        return 'long';
    }
    else if (len < renderProps.minBpCount) {
        return 'short';
    }
    else {
        return 'happy';
    }
}

function getInsertSizePairOrientationType(block, renderProps) {
    const poType = getPairOrientation(block, true);
    if (!poType) {
        return null; // read is not paired or mate is not mapped
    }
    const isType = getInsertSizeType(block, renderProps);

    if (poType === 'LR' && isType === 'happy') { //Orientation and insert size are ok. Use orientation
        return poType;
    }
    else if (poType !== 'LR' && isType === 'happy') { //Bad orientation and good insert size. Use orientation
        return poType;
    }
    else if (poType === 'LR' && isType !== 'happy') { //Good orientation and bad insert size. Use insert size
        return isType;
    }
    else if (poType !== 'LR' && isType !== 'happy') { //All bad, should check inter or intra chr rearrangement
        if (isType === 'otherChr') { // Use insert size
            return isType;
        }
        else { //Use orientation
            return poType;
        }
    }
    else {
        return poType;
    }

}

export function transform(block: ServerBlock, renderProps) {

    const ncigarList = parseNcigar(block.cigarString);
    const minimal = createMinimalRender(block, ncigarList, renderProps.isSoftClipping).map(dumpPart);
    const ncigar = createNcigarRender(block, ncigarList, renderProps.isSoftClipping).map(dumpPart);
    const diffBase = createDiffBaseRender(block).map(dumpPart);
    const softClip = renderProps.isSoftClipping ? createSoftClipRender(block).map(dumpPart) : [];

    const arrows = [
        block.stand === true
            ?
        {
            length: 0,
            startIndex: block.endIndex + 1,
            type: partTypes.rightArrow
        } :
        {
            length: 0,
            startIndex: block.startIndex,
            type: partTypes.leftArrow
        }
    ].map(dumpPart);

    const spec = {
        firstInPair: testBit(block.flagMask, READ_FIRST_IN_PAIR_FLAG),
        insertSize: getInsertSizeType(block, renderProps),
        insertSizeAndPairOrientation: getInsertSizePairOrientationType(block, renderProps),
        lowQ: block.mappingQuality < mappingThreshold,
        mateStrand: testBit(block.flagMask, READ_MATE_REVERSE_STRAND_FLAG) ? 'r' : 'l',
        pair: getPairOrientation(block),
        pairSimplified: getPairOrientation(block, true),
        strand: block.stand ? 'forward' : 'reverse'
    };

    const info = dumpInfo(block, spec);

    return {
        arrows,
        diffBase,
        info,
        minimal,
        ncigar,
        softClip,
        spec
    };
}