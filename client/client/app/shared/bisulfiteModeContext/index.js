const CIGAR_M = 'M';
const CIGAR_S = 'S';
const MODES = {
    CG: /CG/ig,
    CHH: /C[ATC][ATC]/ig,
    GHH: /G[ATC][ATC]/ig,
    CHG: /C[ATC]G/ig,
    HCG: /[ATC]CG/ig,
    GCH: /GC[ATC]/ig,
    WCG: /[AT]CG/ig,
    None: /[CG]/ig,
};
const PAIR = {
    F1R2: 'F1R2',
    F2R1: 'F2R1'
};
const TYPE = {
    METHYLATED: 'METHYLATED',
    UNMETHYLATED: 'UNMETHYLATED'
};
const BASE = {
    C: 'C',
    G: 'G',
    regC: /C/i,
    regG: /G/i,
    A: 'A',
    T: 'T'
};

function getPair(pair) {
    switch (pair) {
        case 'R2L1':
        case 'L1R2':
        case 'L1L2':
        case 'L2L1':
            return PAIR.F1R2;
        case 'R1L2':
        case 'L2R1':
        case 'R1R2':
        case 'R2R1':
            return PAIR.F2R1;
        default:
            return null;
    }
}

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

    constructor () {
        this.getReadSequence = ::this.getRead;
        this.getParts = ::this.getParts;
    }

    getRead(block) {
        const {_items} = this;
        const thisStartIndex = this.startIndex;
        const thisEndIndex = this.endIndex;
        return function() {
            if (!_items) {
                return '';
            }
            const {startIndex, endIndex, cigarString, differentBase, renderDump} = block;
            if (endIndex < thisStartIndex || startIndex > thisEndIndex) {
                return '';
            }
            const softClip = renderDump.softClip;
            const cigarList = cigarString.match(/\d+[MIDNSHP=X]/img);
            const readSequence = [];
            let index = startIndex;
            for (let i = 0; i < cigarList.length; i++) {
                const letter = cigarList[i].slice(-1);
                const count = cigarList[i].slice(0, -1);
                switch (letter) {
                    case CIGAR_M:
                        for (let m = 0; m < count; m++) {
                            readSequence.push(_items[index + m]);
                        }
                        if (differentBase) {
                            for (let d = 0; d < differentBase.length; d++) {
                                readSequence[differentBase[d].relativePosition] = differentBase[d].base;
                            } 
                        }
                        index += count;
                        break;
                    case CIGAR_S:
                        for (let s = 0; s < count; s++) {
                            readSequence.push(softClip[s][3].base);
                        }
                        index += count;
                        break;
                    default:
                        readSequence.push(letter);
                        index += count;
                        break;
                }
            }
            return {
                sequence: readSequence.map(item => item ? item : '-').join(''),
                startIndex,
                endIndex,
                pair: getPair(renderDump.spec.pair)
            };
        };
    }

    getParts(bisulfiteMode, read) {
        const refParts = this.getReferenceParts(read, bisulfiteMode);
        const result = [];
        if (refParts.length) {
            const refBase = read.pair === PAIR.F1R2 ? BASE.regC : BASE.regG;
            for (let i = 0; i < refParts.length; i++) {
                const {refString, refPosition} = refParts[i];
                const position = this.getBasePosition(refString, refBase, bisulfiteMode);
                if (position >= 0) {
                    const baseIndex = refPosition + position + read.startIndex;
                    const readString = read.sequence.slice(refPosition, refPosition + refString.length);
                    if (!readString.includes('-')) {
                        if (refString === readString) {
                            result.push({
                                type: TYPE.METHYLATED,
                                startIndex: baseIndex,
                                endIndex: baseIndex + 1
                            });
                        } else {
                            if (readString === this.getMethylatedString(refBase, refString, bisulfiteMode, true)) {
                                result.push({
                                    type: TYPE.METHYLATED,
                                    startIndex: baseIndex,
                                    endIndex: baseIndex + 1
                                });
                            } else if (readString === this.getMethylatedString(refBase, refString, bisulfiteMode, false)) {
                                result.push({
                                    type: TYPE.UNMETHYLATED,
                                    startIndex: baseIndex,
                                    endIndex: baseIndex + 1
                                });
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    getReferenceParts(read, bisulfiteMode) {
        let refSequence = '';
        for (let i = read.startIndex; i <= read.endIndex; i++) {
            refSequence = refSequence.concat(this._items[i] || '-');
        }
        const isCHH = bisulfiteMode === 'CHH';
        const mode = isCHH ? (
            read.pair === PAIR.F1R2 ? MODES[bisulfiteMode] : MODES.GHH) :
            MODES[bisulfiteMode];
        let value = mode.exec(refSequence);
        const result = [];
        while (value) {
            result.push({
                refPosition: value.index,
                refString: value[0]
            });
            if (isCHH) {
                mode.lastIndex = mode.lastIndex - 2;
            }
            value = mode.exec(refSequence);
        }
        return result;
    }

    getBasePosition(string, base, bisulfiteMode) {
        return bisulfiteMode === 'HCG' ?
            (string.slice(1).search(base) + 1) : string.search(base);
    }

    getMethylatedString(base, string, bisulfiteMode, isMethylated) {
        const basePosition = this.getBasePosition(string, base, bisulfiteMode);
        if (basePosition < 0) return;
        const basesArray = string.split('');
        basesArray[basePosition] = base === BASE.regC ? 
            (isMethylated ? BASE.C : BASE.T) :
            (isMethylated ? BASE.G : BASE.A);
        return basesArray.join('');
    }
}
