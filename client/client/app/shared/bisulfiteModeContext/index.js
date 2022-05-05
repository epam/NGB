const CIGAR = {
    M: 'M',
    S: 'S',
    H: 'H',
    REG: /\d+[MIDNSHP=X]/img
};
const BASE = {
    C: 'c',
    T: 't',
    G: 'g'
};
const FLIP_BASE = {
    A: 't',
    T: 'a',
    C: 'g',
    G: 'c'
};
const SECOND_C = ['HCG', 'WCG', 'GCH', 'NOMeSeq'];
const NONE = {
    name: 'None',
    length: 1
};
const NOME_SEQ = {
    name: 'NOMeSeq',
    length: 3
};
const GCH = 'GCH';
const HCG = 'HCG';
const METHYLATED_MODES = {
    CG: /CG/ig,
    CHH: /C[ATC][ATC]/ig,
    CHG: /C[ATC]G/ig,
    HCG: /[ATC]CG/ig,
    GCH: /GC[ATC]/ig,
    WCG: /[AT]CG/ig,
    None: /[C]/ig,
};
const UNMETHYLATED_MODES = {
    CG: /TG/ig,
    CHH: /T[ATC][ATC]/ig,
    CHG: /T[ATC]G/ig,
    HCG: /[ATC]TG/ig,
    GCH: /GT[ATC]/ig,
    WCG: /[AT]TG/ig,
    None: /[T]/ig,
};
const TYPE = {
    METHYLATED_BASE: 12, // red
    UNMETHYLATED_BASE: 13, // blue
    CYTOSINE_MISMATCH: 14,
    NONCYTOSINE_MISMATCH: 15,
    CG_METHYLATED_BASE: 16, // black
    CG_UNMETHYLATED_BASE: 17, // light yellow
    GC_METHYLATED_BASE: 18, // mint
    GC_UNMETHYLATED_BASE: 19 // light pink
};
const READ_PAIRED_FLAG = 0x1;
const REVERSE_STRAND = 'reverse';

export default class BisulfiteModeContext {
    static instance() {
        return new BisulfiteModeContext();
    }

    _items = null;
    _reverseItems = null;
    startIndex;
    endIndex;

    set items(value) {
        this._items = value;
    }
    set reverseItems(value) {
        this._reverseItems = value;
    }

    // Reconstructs read's sequence by cigar list, different bases (= mismatches)
    // and reference's sequence from read's start index to end of read's length.
    // In case smth not M put '-', for S - multiplies by count, for H - nothing.
    // If read should be fliped, bases are changed to complementary one and sequence are reversed.
    getReadSequence(index, differentBase, cigarList, items, isFlip) {
        let readSequence = [];
        for (let i = 0; i < cigarList.length; i++) {
            const letter = cigarList[i].slice(-1);
            const count = cigarList[i].slice(0, -1);
            switch (letter) {
                case CIGAR.M:
                    for (let m = 0; m < count; m++) {
                        readSequence.push(items[index + m]);
                    }
                    if (differentBase) {
                        for (let d = 0; d < differentBase.length; d++) {
                            const diffBase = differentBase[d].base;
                            const base = isFlip ? FLIP_BASE[diffBase] : diffBase;
                            readSequence[differentBase[d].relativePosition] = base;
                        }
                    }
                    index += count;
                    break;
                case CIGAR.S:
                    for (let s = 0; s < count; s++) {
                        readSequence.push('-');
                    }
                    index += count;
                    break;
                case CIGAR.H:
                    index += count;
                    break;
                default:
                    readSequence.push('-');
                    index += count;
                    break;
            }
        }
        readSequence = isFlip ? readSequence.reverse() : readSequence;
        return readSequence;
    }

    // Gets cigar list of read, should read be fliped, forward or reverse items should be used
    // Based that info gets read's sequence and returns it and other info for read.
    getRead(block) {
        const self = this;
        const {startIndex, endIndex, cigarString, differentBase, renderDump, flagMask} = block;
        if (endIndex < self.startIndex || startIndex > self.endIndex) {
            return null;
        }
        const isFlip = (({strand, firstInPair}, flagMask) => {
            const isPaired = (flagMask & READ_PAIRED_FLAG) !== 0;
            const isStrandNegative = strand === REVERSE_STRAND;
            return isPaired ?
                (isStrandNegative ? firstInPair : !firstInPair) :
                isStrandNegative;
        })(renderDump.spec, flagMask);
        const items = isFlip ? self._reverseItems : self._items;
        if (!items || Object.keys(items).length === 0) {
            return null;
        }
        const cigarList = cigarString.match(CIGAR.REG);
        const readSequence = self.getReadSequence(startIndex, differentBase, cigarList, items, isFlip);
        return {
            sequence: readSequence.map(item => item ? item : '-'),
            startIndex,
            endIndex,
            isFlip,
            differentBase
        };
    }

    // Returns segment of reference's sequence by read coordinates.
    // If read was fliped takes reverse items and reverses its.
    // Fills empty spaces.
    getRefSequence({isFlip, startIndex, endIndex}) {
        let refSequence = [];
        const items = isFlip ? this._reverseItems : this._items;
        for (let i = startIndex; i <= endIndex; i++) {
            refSequence.push(items[i] ? items[i].toLowerCase() : '-');
        }
        refSequence = isFlip ? refSequence.reverse() : refSequence;
        return refSequence;
    }

    // Gets a substring in the sequence by current mode's length and C-base position.
    // E.g.: sequence = AAATCGCCA and position = 4
    // for 'CG' - 'CG'; for 'None' - 'C'; for ('CHH', 'CHG') - 'CGC';
    // for ('HCG', 'GCH', 'WCG', 'NOMeSeq') - 'TCG'.
    getCBaseContext (sequence, i, bisulfiteMode) {
        const startIndex = SECOND_C.includes(bisulfiteMode) ? (i - 1) : i;
        let length = bisulfiteMode.length;
        if (bisulfiteMode === NONE.name) {
            length = NONE.length;
        }
        if (bisulfiteMode === NOME_SEQ.name) {
            length = NOME_SEQ.length;
        }
        const endIndex = startIndex + length;
        if (startIndex < 0 || endIndex > sequence.length) {
            return '';
        }
        const context = sequence.slice(startIndex, endIndex).join('');
        if (context.includes('-')) {
            return '';
        }
        return context.toLowerCase();
    }

    // Returns array of relative positions of C-bases which have context corresponds to current mode
    // For NOMe-Seq mode it's array of arrays with position and exact mode (GCH or HCG)
    getReferenceParts(read, bisulfiteMode) {
        const refSequence = this.getRefSequence(read);
        const result = [];
        if (!refSequence.length) return result;
        for (let i = 0; i < refSequence.length; i++) {
            let context, match;
            let nomeSeqMatchGCH, nomeSeqMatchHCG;
            switch (refSequence[i]) {
                case BASE.C:
                    context = this.getCBaseContext(refSequence, i, bisulfiteMode);
                    if (context) {
                        if (bisulfiteMode === NOME_SEQ.name) {
                            nomeSeqMatchGCH = context.match(METHYLATED_MODES.GCH);
                            nomeSeqMatchHCG = context.match(METHYLATED_MODES.HCG);
                            if (nomeSeqMatchGCH) {
                                result.push([GCH, i]);
                            }
                            if (nomeSeqMatchHCG) {
                                result.push([HCG, i]);
                            }
                        } else {
                            match = context.match(METHYLATED_MODES[bisulfiteMode]);
                            if (match) {
                                result.push(i);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    // Gets methylated and unmethylated bases and cytosine and noncytosine mismatches of read,
    // which need to be colored corespond to bisulfite mode.
    getReadParts(bisulfiteMode, read) {
        let result = [];
        if (!read) {
            return result;
        }
        const refParts = this.getReferenceParts(read, bisulfiteMode);
        if (refParts.length) {
            result = result.concat(this.getReadMethBase(refParts, read, bisulfiteMode));
        }
        if (read.differentBase) {
            result = result.concat(this.getReadCytosineMismatch(read));
        }
        return result;
    }

    // Base is methylated if on the position, where the reference has C, the read has C,
    // and unmethylated - when the read has T.
    // For NOMe-Seq mode colors are different, so types are different too.
    getMethylationType(isNomeSeq, mode, readContext, cBase) {
        let type;
        if (isNomeSeq) {
            if (mode === GCH &&
                (readContext.match(METHYLATED_MODES.GCH) ||
                readContext.match(UNMETHYLATED_MODES.GCH))
            ) {
                if (cBase === BASE.C) {
                    type = TYPE.GC_METHYLATED_BASE;
                }
                if (cBase === BASE.T) {
                    type = TYPE.GC_UNMETHYLATED_BASE;
                }
            }
            if (mode === HCG &&
                (readContext.match(METHYLATED_MODES.HCG) ||
                readContext.match(UNMETHYLATED_MODES.HCG))
            ) {
                if (cBase === BASE.C) {
                    type = TYPE.CG_METHYLATED_BASE;
                }
                if (cBase === BASE.T) {
                    type = TYPE.CG_UNMETHYLATED_BASE;
                }
            }
        } else {
            if (
                readContext.match(METHYLATED_MODES[mode]) ||
                readContext.match(UNMETHYLATED_MODES[mode])
            ) {
                if (cBase === BASE.C) {
                    type = TYPE.METHYLATED_BASE;
                }
                if (cBase === BASE.T) {
                    type = TYPE.UNMETHYLATED_BASE;
                }
            }
        }
        return type;
    }

    // For each reference's  C-base's position in the read's sequence,
    // the context is checked and, if it matches the mode,
    // the corresponding objects are returned for methylated and unmethylated bases.
    // For fliped reads relative position is reculculated.
    getReadMethBase(refParts, read, bisulfiteMode) {
        const result = [];
        const isNomeSeq = bisulfiteMode === NOME_SEQ.name;
        for (let i = 0; i < refParts.length; i++) {
            const [mode, position] = isNomeSeq ? refParts[i] : [bisulfiteMode, refParts[i]];
            const readContext = this.getCBaseContext(read.sequence, position, mode);
            if (readContext) {
                const cPosition = SECOND_C.includes(bisulfiteMode) ? 1 : 0;
                const cBase = readContext[cPosition];
                const baseIndex = read.isFlip ? (read.endIndex - position) : (position + read.startIndex);
                const type = this.getMethylationType(isNomeSeq, mode, readContext, cBase);
                if (type) {
                    result.push({
                        type,
                        startIndex: baseIndex,
                        endIndex: baseIndex + 1
                    });
                }
            }
        }
        return result;
    }

    // Every read's different base is checked is it cytosine (guanine for fliped case).
    // CYTOSINE_MISMATCH and NONCYTOSINE_MISMATCH have different color on track.
    getReadCytosineMismatch(read) {
        const {isFlip, startIndex, endIndex, differentBase} = read;
        const result = [];
        for (let i = 0; i < differentBase.length; i++) {
            let type = TYPE.NONCYTOSINE_MISMATCH;
            const {relativePosition, base} = differentBase[i];
            const flipRelativePosition = (endIndex - startIndex) - relativePosition;
            const baseIndex = isFlip ?
                (endIndex - flipRelativePosition) :
                (relativePosition + startIndex);
            if (
                (isFlip && base.toLowerCase() === BASE.G) ||
                (!isFlip && base.toLowerCase() === BASE.C)
            ) { type = TYPE.CYTOSINE_MISMATCH; }
            result.push({
                base,
                length: 1,
                type,
                startIndex: baseIndex,
                endIndex: baseIndex + 1
            });
        }
        return result;
    }
}
