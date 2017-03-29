import {sortTypes, partTypes} from '../../../modes';
import {Line} from '../line';
import {Sorting} from '../../../../../utilities';
import {undumpPart} from '../../internalUtilities';

function generateValueFnDefaultSortingMode() {
    return function(item) {
        return item.initialLineIndex;
    };
}

function generateValueFnSortByMappingQuality(viewAsPairs, position) {
    return function(item) {
        if (item.isPairedReads && viewAsPairs) {
            if (item.leftPair.endIndex >= position)
                return item.leftPair.mappingQuality;
            return item.rightPair.mappingQuality;
        }
        return item.mappingQuality;
    };
}

function generateValueFnSortByStartLocation(viewAsPairs, position) {
    return function(item) {
        if (item.isPairedReads && viewAsPairs) {
            if (item.leftPair.endIndex >= position)
                return item.leftPair.startIndex;
            return item.rightPair.endIndex;
        }
        return item.startIndex;
    };
}

function generateValueFnSortByStrand(viewAsPairs, position) {
    return function(item) {
        if (item.isPairedReads && viewAsPairs) {
            if (item.leftPair.endIndex >= position)
                return item.leftPair.renderDump.spec.strand.toLowerCase() === 'reverse' ? 0 : 1;
            return item.rightPair.renderDump.spec.strand.toLowerCase() === 'reverse' ? 0 : 1;
        }
        return item.renderDump.spec.strand.toLowerCase() === 'reverse' ? 0 : 1;
    };
}

const DELETION_BASE = 'deletion';
const SORT_BY_BASE_ORDERS = [DELETION_BASE, 'a', 'c', 'g', 't'];

function generateValueFnSortByBase(viewAsPairs, position) {

    const getRead = function(item) {
        if (item.isPairedReads && viewAsPairs) {
            if (item.leftPair.endIndex >= position)
                return item.leftPair;
            else if (item.rightPair.startIndex <= position)
                return item.rightPair;
            else
                return null;
        }
        return item;
    };
    const checkPart = function(part, type) {
        if (part.type === type && part.startIndex <= position && part.endIndex > position) {
            return part;
        }
        return null;
    };
    const checkDeletion = function(readItem) {
        if (readItem.renderDump.ncigar) {
            const undumpedNcigar = readItem.renderDump.ncigar.map(undumpPart);
            for (let i = 0; i < undumpedNcigar.length; i++) {
                if (checkPart(undumpedNcigar[i], partTypes.deletion)) {
                    return true;
                }
            }
        }
        return false;
    };
    const sortByBase = function(readItem) {
        let base = null;
        if (readItem.renderDump.diffBase) {
            const undumpedDiffBase = readItem.renderDump.diffBase.map(undumpPart);
            for (let i = 0; i < undumpedDiffBase.length; i++) {
                const part = checkPart(undumpedDiffBase[i], partTypes.base);
                if (part) {
                    base = part.base.toLowerCase();
                    break;
                }
            }
        }
        if (!base && readItem.renderDump.softClip) {
            const undumpedSoftClipBase = readItem.renderDump.softClip.map(undumpPart);
            for (let i = 0; i < undumpedSoftClipBase.length; i++) {
                const part = checkPart(undumpedSoftClipBase[i], partTypes.softClipBase);
                if (part) {
                    base = part.base.toLowerCase();
                    break;
                }
            }
        }
        const index = SORT_BY_BASE_ORDERS.indexOf(base);
        if (index === -1) {
            return Infinity;
        }
        return index;
    };

    return function(item) {
        const readItem = getRead(item);
        if (!readItem || !readItem.renderDump) {
            return Infinity;
        }
        if (checkDeletion(readItem)) {
            return SORT_BY_BASE_ORDERS.indexOf(DELETION_BASE);
        }
        return sortByBase(readItem);
    };
}

function generateValueFnSortByInsertSize() {
    return function(item) {
        return Math.abs(item.tlen);
    };
}

function generateValueFn(mode, viewAsPairs, position) {
    switch (mode) {
        default:
        case sortTypes.defaultSortingMode: return generateValueFnDefaultSortingMode();
        case sortTypes.sortByBase: return generateValueFnSortByBase(viewAsPairs, position);
        case sortTypes.sortByInsertSize: return generateValueFnSortByInsertSize();
        case sortTypes.sortByMappingQuality: return generateValueFnSortByMappingQuality(viewAsPairs, position);
        case sortTypes.sortByStartLocation: return generateValueFnSortByStartLocation(viewAsPairs, position);
        case sortTypes.sortByStrand: return generateValueFnSortByStrand(viewAsPairs, position);
    }
}

export function sort(groups, mode, position, viewAsPairs) {
    const findMatchingReadCriteria = function(item) {
        if (mode === sortTypes.defaultSortingMode)
            return true;
        if (item.isPairedReads && viewAsPairs) {
            return item.startIndex <= position && item.endIndex >= position &&
                !(item.leftPair.endIndex < position && item.rightPair.startIndex > position);
        }
        return item.startIndex <= position && item.endIndex >= position;
    };
    const ascending = mode !== sortTypes.sortByInsertSize && mode !== sortTypes.sortByMappingQuality;
    const valueFn = generateValueFn(mode, viewAsPairs, position);
    for (let g = 0; g < groups.length; g++) {
        let linesCount = 0;
        let linesArray = groups[g].lines;
        if (viewAsPairs) {
            linesArray = groups[g].pairedLines;
        }
        linesArray.forEach((line: Line, index) => {
            const findResult = line.findEntries(findMatchingReadCriteria);
            if (findResult && findResult.length >= 1) {
                linesCount++;
                line.sortData = {additional: line.initialIndex, index, value: valueFn(findResult.toArray()[0])};
            } else {
                line.sortData = {additional: line.initialIndex, index, value: ascending ? Infinity : -Infinity};
            }
        });
        if (linesCount > 0) {
            Sorting.quickSortWithComparison(linesArray, (a, b) => {
                if (ascending) {
                    if (a.sortData.value === b.sortData.value) {
                        return a.sortData.additional < b.sortData.additional;
                    }
                    return a.sortData.value < b.sortData.value;
                }
                else {
                    if (a.sortData.value === b.sortData.value) {
                        return a.sortData.additional > b.sortData.additional;
                    }
                    return a.sortData.value > b.sortData.value;
                }
            });
            linesArray.forEach((line: Line, index) => {
                line.setNewIndex(index);
            });
            if (viewAsPairs) {
                groups[g].pairedLines = linesArray;
            }
            else {
                groups[g].lines = linesArray;
            }
        }
    }
}