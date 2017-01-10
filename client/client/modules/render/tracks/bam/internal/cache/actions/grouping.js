import {groupModes} from '../../../modes';

const GROUP_NAME_DEFAULT = '*';
const GROUP_DISPLAY_NAME_NOT_AVAILABLE = 'not available';

function getGroupNamesForGroupByDefaultMode() {
    return {
        groupDisplayName: GROUP_DISPLAY_NAME_NOT_AVAILABLE,
        groupName: GROUP_NAME_DEFAULT
    };
}

function getGroupNamesForGroupByChromosomeOfMateMode(read) {
    let groupDisplayName = GROUP_DISPLAY_NAME_NOT_AVAILABLE;
    let groupName = GROUP_NAME_DEFAULT;
    if (read.rname) {
        groupDisplayName = read.rname;
    }
    if (read.rnext && read.rname) {
        if (read.rnext !== read.rname) {
            groupName = read.rnext.toLowerCase();
            groupDisplayName = groupName;
        }
    }
    return {
        groupDisplayName,
        groupName
    };
}

function getGroupNamesForGroupByFirstInPairMode(read) {
    let groupDisplayName = GROUP_DISPLAY_NAME_NOT_AVAILABLE;
    let groupName = GROUP_NAME_DEFAULT;
    if (read.renderDump.spec.pair !== 'NA' && read.renderDump.spec.pair) {
        switch (read.renderDump.spec.pair.toUpperCase()) {
            case 'R2L1':
            case 'L1R2':
            case 'L1L2':
            case 'L2L1':
                groupName = 'l';
                groupDisplayName = 'forward strand';
                break;
            case 'R1L2':
            case 'L2R1':
            case 'R1R2':
            case 'R2R1':
                groupName = 'r';
                groupDisplayName = 'reverse strand';
                break;
            default:
                break;
        }
    }
    return {
        groupDisplayName,
        groupName
    };
}

function getGroupNamesForGroupByPairOrientationMode(read) {
    let groupDisplayName = GROUP_DISPLAY_NAME_NOT_AVAILABLE;
    let groupName = GROUP_NAME_DEFAULT;
    if (read.renderDump.spec.pair !== 'NA') {
        groupName = read.renderDump.spec.pairSimplified.toLowerCase();
        groupDisplayName = groupName;
    }
    return {
        groupDisplayName,
        groupName
    };
}

function getGroupNamesForGroupByReadStrandMode(read) {
    let groupDisplayName = GROUP_DISPLAY_NAME_NOT_AVAILABLE;
    const groupName = read.renderDump.spec.strand.toLowerCase();
    if (groupName === 'l') {
        groupDisplayName = 'forward strand';
    }
    else {
        groupDisplayName = 'reverse strand';
    }
    return {
        groupDisplayName,
        groupName
    };
}

export function getGroupNamesFn(mode): Function {
    switch (mode) {
        default:
        case groupModes.defaultGroupingMode:
            return getGroupNamesForGroupByDefaultMode;
        case groupModes.groupByChromosomeOfMateMode:
            return getGroupNamesForGroupByChromosomeOfMateMode;
        case groupModes.groupByFirstInPairMode:
            return getGroupNamesForGroupByFirstInPairMode;
        case groupModes.groupByPairOrientationMode:
            return getGroupNamesForGroupByPairOrientationMode;
        case groupModes.groupByReadStrandMode:
            return getGroupNamesForGroupByReadStrandMode;
    }
}

export function group(groups, mode) {
    const newGroups = [];
    let newGroupNames = [];
    const getGroupNames = getGroupNamesFn(mode);
    for (let g = 0; g < groups.length; g++) {
        const group = groups[g];
        for (let i = 0; i < group.rawReads.length; i++) {
            const read = group.rawReads[i];
            const {groupDisplayName, groupName} = getGroupNames(read);
            let index = newGroupNames.indexOf(groupName);
            if (index === -1) {
                newGroups.push({
                    displayName: groupDisplayName,
                    lines: [],
                    name: groupName,
                    pairedLines: [],
                    rawPairedReads: [],
                    rawReads: []
                });
                index = newGroups.length - 1;
                newGroupNames = newGroups.map(x => x.name);
            }
            newGroups[index].rawReads.push(read);
        }
        group.lines = null;
        group.pairedLines = null;
        group.rawReads = null;
        group.rawPairedReads = null;
    }
    newGroupNames = null;
    return newGroups;
}