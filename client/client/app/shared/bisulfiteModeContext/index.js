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
const SECOND_C = ['HCG', 'WCG', 'GCH'];
const NONE = 'None';
const MODES = {
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
    METHYLATED_BASE: 12,
    UNMETHYLATED_BASE: 13,
    CYTOSINE_MISMATCH: 14,
    NONCYTOSINE_MISMATCH: 15
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

    getRead(block) {
        const self = this;
        return function() {
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
            if (!items) {
                return null;
            }
            const cigarList = cigarString.match(CIGAR.REG);
            let readSequence = [];

            let index = startIndex;
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
            return {
                sequence: readSequence.map(item => item ? item : '-'),
                startIndex,
                endIndex,
                isFlip,
                differentBase
            };
        };
    }

    getRefSequence({isFlip, startIndex, endIndex}) {
        let refSequence = [];
        const items = isFlip ? this._reverseItems : this._items;
        for (let i = startIndex; i <= endIndex; i++) {
            refSequence.push(items[i] ? items[i].toLowerCase() : '-');
        }
        refSequence = isFlip ? refSequence.reverse() : refSequence;
        return refSequence;
    }

    getString (sequence, i, bisulfiteMode) {
        const startIndex = SECOND_C.includes(bisulfiteMode) ? (i > 0 ? (i - 1) : (-1)) : i;
        const length = bisulfiteMode === NONE ? 1 : bisulfiteMode.length;
        const endIndex = startIndex + length;
        if (startIndex < 0 || endIndex > sequence.length) {
            return '';
        }
        const string = sequence.slice(startIndex, endIndex).join('');
        if (string.includes('-')) {
            return '';
        }
        return string.toLowerCase();
    }

    getReferenceParts(read, bisulfiteMode) {
        const refSequence = this.getRefSequence(read);
        const result = [];
        if (!refSequence.length) return result;
        for (let i = 0; i < refSequence.length; i++) {
            let match, string;
            switch (refSequence[i]) {
                case BASE.C:
                    string = this.getString(refSequence, i, bisulfiteMode);
                    if (string) {
                        match = string.match(MODES[bisulfiteMode]);
                    }
                    if (match) {
                        result.push(i);
                    }
                    break;
                default:
                    break;
            }
        }
        return result;
    }

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

    getReadMethBase(refParts, read, bisulfiteMode) {
        const result = [];
        for (let i = 0; i < refParts.length; i++) {
            const position = refParts[i];
            const readString = this.getString(read.sequence, position, bisulfiteMode);
            if (readString) {
                if (readString.match(MODES[bisulfiteMode]) || readString.match(UNMETHYLATED_MODES[bisulfiteMode])) {
                    const cPosition = SECOND_C.includes(bisulfiteMode) ? 1 : 0;
                    const cBase = readString[cPosition];
                    const baseIndex = read.isFlip ? (read.endIndex - position) : (position + read.startIndex);
                    if (cBase === BASE.C) {
                        result.push({
                            type: TYPE.METHYLATED_BASE,
                            startIndex: baseIndex,
                            endIndex: baseIndex + 1
                        });
                    }
                    if (cBase === BASE.T) {
                        result.push({
                            type: TYPE.UNMETHYLATED_BASE,
                            startIndex: baseIndex,
                            endIndex: baseIndex + 1
                        });
                    }
                }
            }
        }
        return result;
    }

    getReadCytosineMismatch(read) {
        const {isFlip, startIndex, endIndex, differentBase} = read;
        const result = [];
        for (let i = 0; i < differentBase.length; i++) {
            let type = TYPE.NONCYTOSINE_MISMATCH;
            const {relativePosition, base} = differentBase[i];
            const flipRelativePosition = read.sequence.length - relativePosition - 1;
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
