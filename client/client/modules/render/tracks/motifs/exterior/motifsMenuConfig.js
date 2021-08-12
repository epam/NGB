import commonMenu from '../../common/menu';
import {getDivider} from '../../../utilities/menu';

function colorPerform (tracks, options) {
    const [dispatcher] = (tracks || [])
        .map(track => track.config.dispatcher)
        .filter(Boolean);
    function getCommonColor (colors) {
        const set = new Set(colors);
        if (set.size === 1) {
            return [...set][0];
        }
        return undefined;
    }
    if (dispatcher) {
        const strand = (tracks || []).map(track => track.motifStrand)[0];
        const configColors = (tracks || [])
            .map(track =>  track.motifStrand &&
                track.trackConfig &&
                track.trackConfig.matches &&
                track.trackConfig.matches.defaultColor
                    ? track.trackConfig.matches.defaultColor[track.motifStrand]
                    : undefined
            );
        const stateColors = (tracks || [])
            .map(track => (track.state.color || {})[strand]);
        dispatcher.emitSimpleEvent('motifs:color:configure', {
            config: {
                color: getCommonColor(stateColors) || getCommonColor(configColors),
                defaultColor: getCommonColor(configColors)
            },
            options,
            sources: (tracks || []).map(track => track.config.name),
            strand,
            types: [...(new Set((tracks || []).map(track => track.config.format)))]
        });
    }
}

const [resize, header] = commonMenu;

export default [
    {
        fields: [
            {
                label: 'Color',
                name: 'motifs>general>color',
                perform: colorPerform,
                type: 'button'
            },
            getDivider(),
            resize,
            header
        ],
        label:'General',
        name:'motifs>general',
        type: 'submenu',
    }
];
