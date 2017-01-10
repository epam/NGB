import {colorModes} from '../modes';
import {getDivider} from './divider';

export default {
    displayAdditionalName: state => {
        if (state.shadeByQuality) {
            return 'Shade by quality';
        }
    },
    displayName: state => {
        switch (state.colorMode) {
            case colorModes.noColor:
                return 'No color';
            case colorModes.byPairOrientation:
                return 'By pair orientation';
            case colorModes.byInsertSize:
                return 'By insert size';
            case colorModes.byInsertSizeAndPairOrientation:
                return 'By insert size and pair orientation';
            case colorModes.byReadStrand:
                return 'By read strand';
            case colorModes.byFirstInPairStrand:
                return 'By first in pair strand';
        }
    },
    fields: [
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => state.colorMode = colorModes.noColor,
            isEnabled: state => state.colorMode === colorModes.noColor,
            label: 'No color',
            name: 'bam>color>noColor',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => state.colorMode = colorModes.byPairOrientation,
            isEnabled: state => state.colorMode === colorModes.byPairOrientation,
            label: 'By pair orientation',
            name: 'bam>color>pairOrientation',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => state.colorMode = colorModes.byInsertSize,
            isEnabled: state => state.colorMode === colorModes.byInsertSize,
            label: 'By insert size',
            name: 'bam>color>insertSize',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => state.colorMode = colorModes.byInsertSizeAndPairOrientation,
            isEnabled: state => state.colorMode === colorModes.byInsertSizeAndPairOrientation,
            label: 'By insert size and pair orientation',
            name: 'bam>color>insertSizeAndPairOrientation',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => state.colorMode = colorModes.byReadStrand,
            isEnabled: state => state.colorMode === colorModes.byReadStrand,
            label: 'By read strand',
            name: 'bam>color>readStrand',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => state.colorMode = colorModes.byFirstInPairStrand,
            isEnabled: state => state.colorMode === colorModes.byFirstInPairStrand,
            label: 'By first in pair strand',
            name: 'bam>color>firstInPairStrand',
            type: 'checkbox'
        },
        getDivider(),
        {
            disable: state => state.shadeByQuality = false,
            enable: state => state.shadeByQuality = true,
            isEnabled: state => state.shadeByQuality,
            label: 'Shade by quality',
            name: 'bam>color>shadeByQuality',
            type: 'checkbox'
        }
    ],
    label: 'Color mode',
    name: 'bam>color',
    type: 'submenu'
};