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
        const configColors = (tracks || [])
            .map(track => track.trackConfig && track.trackConfig.bed
                ? track.trackConfig.bed.defaultColor
                : undefined
            );
        const stateColors = (tracks || [])
            .map(track => track.state.color);
        dispatcher.emitSimpleEvent('bed:color:configure', {
            config: {
                color: getCommonColor(stateColors) || getCommonColor(configColors),
                defaultColor: getCommonColor(configColors)
            },
            options,
            sources: (tracks || []).map(track => track.config.name),
            types: [...(Array.from(new Set((tracks || []).map(track => track.config.format))))]
        });
    }
}

export default {
    fields:[
        {
            label: 'Color',
            name: 'bed>general>color',
            perform: colorPerform,
            type: 'button'
        },
        getDivider(),
        ...commonMenu
    ],
    label:'General',
    name:'bed>general',
    type: 'submenu'
};
