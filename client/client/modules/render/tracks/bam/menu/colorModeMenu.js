import {colorModes} from '../modes';
import {menu} from '../../../utilities';

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
            case colorModes.byBisulfiteConversion:
                switch(state.bisulfiteMode) {
                    case colorModes.bisulfiteMode.CG:
                        return 'By bisulfite conversion: CG';
                    case colorModes.bisulfiteMode.CHH:
                        return 'By bisulfite conversion: CHH';
                    case colorModes.bisulfiteMode.CHG:
                        return 'By bisulfite conversion: CHG';
                    case colorModes.bisulfiteMode.HCG:
                        return 'By bisulfite conversion: HCG';
                    case colorModes.bisulfiteMode.GCH:
                        return 'By bisulfite conversion: GCH';
                    case colorModes.bisulfiteMode.WCG:
                        return 'By bisulfite conversion: WCG';
                    case colorModes.bisulfiteMode.None:
                        return 'By bisulfite conversion: None';
                    case colorModes.bisulfiteMode.NOMeSeq:
                        return 'By bisulfite conversion: NOMe-Seq';
                }
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
        menu.getDivider(),
        {
            label: 'By bisulfite conversion',
            type: 'text'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => {
                state.colorMode = colorModes.byBisulfiteConversion;
                state.bisulfiteMode = colorModes.bisulfiteMode.CG;
            },
            isEnabled: state => (
                state.colorMode === colorModes.byBisulfiteConversion &&
                state.bisulfiteMode === colorModes.bisulfiteMode.CG
            ),
            label: 'CG',
            name: 'bam>color>bisulfiteConversion>CG',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => {
                state.colorMode = colorModes.byBisulfiteConversion;
                state.bisulfiteMode = colorModes.bisulfiteMode.CHH;
            },
            isEnabled: state => (
                state.colorMode === colorModes.byBisulfiteConversion &&
                state.bisulfiteMode === colorModes.bisulfiteMode.CHH
            ),
            label: 'CHH',
            name: 'bam>color>bisulfiteConversion>CHH',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => {
                state.colorMode = colorModes.byBisulfiteConversion;
                state.bisulfiteMode = colorModes.bisulfiteMode.CHG;
            },
            isEnabled: state => (
                state.colorMode === colorModes.byBisulfiteConversion &&
                state.bisulfiteMode === colorModes.bisulfiteMode.CHG
            ),
            label: 'CHG',
            name: 'bam>color>bisulfiteConversion>CHG',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => {
                state.colorMode = colorModes.byBisulfiteConversion;
                state.bisulfiteMode = colorModes.bisulfiteMode.HCG;
            },
            isEnabled: state => (
                state.colorMode === colorModes.byBisulfiteConversion &&
                state.bisulfiteMode === colorModes.bisulfiteMode.HCG
            ),
            label: 'HCG',
            name: 'bam>color>bisulfiteConversion>HCG',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => {
                state.colorMode = colorModes.byBisulfiteConversion;
                state.bisulfiteMode = colorModes.bisulfiteMode.GCH;
            },
            isEnabled: state => (
                state.colorMode === colorModes.byBisulfiteConversion &&
                state.bisulfiteMode === colorModes.bisulfiteMode.GCH
            ),
            label: 'GCH',
            name: 'bam>color>bisulfiteConversion>GCH',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => {
                state.colorMode = colorModes.byBisulfiteConversion;
                state.bisulfiteMode = colorModes.bisulfiteMode.WCG;
            },
            isEnabled: state => (
                state.colorMode === colorModes.byBisulfiteConversion &&
                state.bisulfiteMode === colorModes.bisulfiteMode.WCG
            ),
            label: 'WCG',
            name: 'bam>color>bisulfiteConversion>WCG',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => {
                state.colorMode = colorModes.byBisulfiteConversion;
                state.bisulfiteMode = colorModes.bisulfiteMode.None;
            },
            isEnabled: state => (
                state.colorMode === colorModes.byBisulfiteConversion &&
                state.bisulfiteMode === colorModes.bisulfiteMode.None
            ),
            label: 'None',
            name: 'bam>color>bisulfiteConversion>None',
            type: 'checkbox'
        },
        {
            disable: state => state.colorMode = colorModes.noColor,
            enable: state => {
                state.colorMode = colorModes.byBisulfiteConversion;
                state.bisulfiteMode = colorModes.bisulfiteMode.NOMeSeq;
            },
            isEnabled: state => (
                state.colorMode === colorModes.byBisulfiteConversion &&
                state.bisulfiteMode === colorModes.bisulfiteMode.NOMeSeq
            ),
            label: 'NOMeSeq',
            name: 'bam>color>bisulfiteConversion>NOMeSeq',
            type: 'checkbox'
        },
        menu.getDivider(),
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
