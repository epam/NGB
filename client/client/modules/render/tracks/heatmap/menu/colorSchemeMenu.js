import {HeatmapColorSchemes} from '../../../heatmap';
import {getDivider} from '../../../utilities/menu';

function perform (tracks) {
    const [track] = tracks || [];
    if (!track) {
        return;
    }
    if (track.state && track.state.heatmap && track.state.heatmap.colorScheme) {
        track.state.heatmap.colorScheme.configureRequest();
    }
}

function setColorSchemeMode(state, mode) {
    if (state && state.heatmap && state.heatmap.colorScheme) {
        state.heatmap.colorScheme.type = mode;
    }
}

function getColorSchemeMode(state) {
    if (state && state.heatmap && state.heatmap.colorScheme) {
        return state.heatmap.colorScheme.type;
    }
    return undefined;
}

function getColorSchemeModeAvailable(state, mode) {
    if (state && state.heatmap && state.heatmap.colorScheme) {
        return state.heatmap.colorScheme.colorSchemeAvailable(mode);
    }
    return false;
}

export default {
    displayName: state => {
        if (state.heatmap && state.heatmap.colorScheme) {
            switch (state.heatmap.colorScheme.type) {
                case HeatmapColorSchemes.continuous:
                    return 'Continuous';
                case HeatmapColorSchemes.discrete: {
                    return 'Discrete';
                }
            }
        }
        return undefined;
    },
    fields: [
        {
            disable: state => setColorSchemeMode(state, HeatmapColorSchemes.discrete),
            enable: state => setColorSchemeMode(state, HeatmapColorSchemes.continuous),
            isEnabled: state => getColorSchemeMode(state) === HeatmapColorSchemes.continuous,
            label: 'Continuous',
            name: 'heatmap>scheme>continuous',
            type: 'checkbox',
            disabled: state => !getColorSchemeModeAvailable(state, HeatmapColorSchemes.continuous)
        },
        {
            disable: state => setColorSchemeMode(state, HeatmapColorSchemes.continuous),
            enable: state => setColorSchemeMode(state, HeatmapColorSchemes.discrete),
            isEnabled: state => getColorSchemeMode(state) === HeatmapColorSchemes.discrete,
            label: 'Discrete',
            name: 'heatmap>scheme>discrete',
            type: 'checkbox',
            disabled: state => !getColorSchemeModeAvailable(state, HeatmapColorSchemes.discrete)
        },
        getDivider(),
        {
            perform,
            label: 'Configure',
            name: 'heatmap>scheme>configure',
            type: 'button'
        },
    ],
    label: 'Color Scheme',
    name: 'heatmap>scheme',
    type: 'submenu',
    isVisible: (state, tracks) => tracks.length === 1
};
