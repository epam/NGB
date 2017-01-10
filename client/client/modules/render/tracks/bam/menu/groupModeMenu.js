import {groupModes} from '../modes';

export default {
    displayName: state => {
        switch (state.groupMode) {
            case groupModes.defaultGroupingMode:
                return 'Default';
            case groupModes.groupByFirstInPairMode:
                return 'By first in pair';
            case groupModes.groupByPairOrientationMode:
                return 'By pair orientation';
            case groupModes.groupByChromosomeOfMateMode:
                return 'By chromosome of mate';
            case groupModes.groupByReadStrandMode:
                return 'By read strand';
        }
    },
    fields: [
        {
            disable: state => state.groupMode = groupModes.defaultGroupingMode,
            enable: state => state.groupMode = groupModes.defaultGroupingMode,
            isEnabled: state => state.groupMode === groupModes.defaultGroupingMode,
            label: 'Default',
            name: 'bam>group>default',
            type: 'checkbox'
        },
        {
            disable: state => state.groupMode = groupModes.defaultGroupingMode,
            enable: state => state.groupMode = groupModes.groupByFirstInPairMode,
            isEnabled: state => state.groupMode === groupModes.groupByFirstInPairMode,
            label: 'By first in pair strand',
            name: 'bam>group>firstInPairStrand',
            type: 'checkbox'
        },
        {
            disable: state => state.groupMode = groupModes.defaultGroupingMode,
            enable: state => state.groupMode = groupModes.groupByPairOrientationMode,
            isEnabled: state => state.groupMode === groupModes.groupByPairOrientationMode,
            label: 'By pair orientation',
            name: 'bam>group>pairOrientation',
            type: 'checkbox'
        },
        {
            disable: state => state.groupMode = groupModes.defaultGroupingMode,
            enable: state => state.groupMode = groupModes.groupByChromosomeOfMateMode,
            isEnabled: state => state.groupMode === groupModes.groupByChromosomeOfMateMode,
            label: 'By chromosome of mate',
            name: 'bam>group>chromosomeOfMate',
            type: 'checkbox'
        },
        {
            disable: state => state.groupMode = groupModes.defaultGroupingMode,
            enable: state => state.groupMode = groupModes.groupByReadStrandMode,
            isEnabled: state => state.groupMode === groupModes.groupByReadStrandMode,
            label: 'By read strand',
            name: 'bam>group>readStrand',
            type: 'checkbox'
        }
    ],
    label: 'Group',
    name: 'bam>group',
    type: 'submenu'
};