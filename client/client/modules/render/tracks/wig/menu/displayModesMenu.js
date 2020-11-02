import {displayModes} from '../modes';

export default {
    displayName: state => {
        switch (state.coverageDisplayMode) {
            case displayModes.defaultDisplayMode:
                return 'Bar Graph';
            case displayModes.heatMapDisplayMode:
                return 'Heat Map';
        }
    },
    fields: [
        {
            disable: state => state.coverageDisplayMode = displayModes.defaultDisplayMode,
            enable: state => state.coverageDisplayMode = displayModes.defaultDisplayMode,
            isEnabled: state => state.coverageDisplayMode === displayModes.defaultDisplayMode,
            label: 'Bar Graph',
            name: 'coverage>display>default',
            type: 'checkbox'
        },
        {
            disable: state => state.coverageDisplayMode = displayModes.heatMapDisplayMode,
            enable: state => {
                state.coverageDisplayMode = displayModes.heatMapDisplayMode;
                state.coverageLogScale = false;
            },
            isEnabled: state => state.coverageDisplayMode === displayModes.heatMapDisplayMode,
            label: 'Heat Map',
            name: 'coverage>scale>heatmap',
            type: 'checkbox'
        }
    ],
    label: 'Display',
    name: 'coverage>display',
    type: 'submenu',
    isVisible: () => true
};