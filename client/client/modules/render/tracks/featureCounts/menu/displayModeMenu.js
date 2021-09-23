import {getDivider} from '../../../utilities/menu';
import {displayModes} from '../modes';

const colorsConfigAvailable = (state, tracks, track) => state.featureCountsDisplayMode === displayModes.barChart &&
    track.fcSourcesManager &&
    track.fcSourcesManager.sources.length;
const colorsConfigAvailableForSources = (state, tracks, track) => colorsConfigAvailable(state, tracks, track) &&
    track.fcSourcesManager.sources.length > 1;

export default {
    displayAdditionalName: state => {
        if (state.featureCountsDisplayMode === displayModes.barChart) {
            const parts = [];
            if (state.singleBarChartColors) {
                parts.push('Single color');
            }
            if (state.grayScaleColors) {
                parts.push('Grayscale');
            }
            if (parts.length > 0) {
                return parts.join('/');
            }
        }
        return undefined;
    },
    displayName: state => {
        switch (state.featureCountsDisplayMode) {
            case displayModes.features:
                return 'Features';
            case displayModes.barChart: {
                return 'Bar Graph';
            }
        }
    },
    fields: [
        {
            disable: state => state.featureCountsDisplayMode = displayModes.features,
            enable: state => state.featureCountsDisplayMode = displayModes.features,
            isEnabled: state => state.featureCountsDisplayMode === displayModes.features,
            label: 'Features',
            name: 'featurecounts>display>default',
            type: 'checkbox'
        },
        {
            disable: state => state.featureCountsDisplayMode = displayModes.features,
            enable: state => state.featureCountsDisplayMode = displayModes.barChart,
            isEnabled: state => state.featureCountsDisplayMode === displayModes.barChart,
            label: 'Bar Graph',
            name: 'featurecounts>display>barchart',
            type: 'checkbox'
        },
        getDivider({isVisible: colorsConfigAvailable}),
        {
            disable: state => state.singleBarChartColors = false,
            enable: state => state.singleBarChartColors = true,
            isEnabled: state => !!state.singleBarChartColors,
            isVisible: colorsConfigAvailableForSources,
            label: 'Use single color for sources',
            name: 'featurecounts>display>singleColor',
            type: 'checkbox'
        },
        {
            disable: state => state.grayScaleColors = false,
            enable: state => state.grayScaleColors = true,
            isEnabled: state => !!state.grayScaleColors,
            isVisible: colorsConfigAvailable,
            label: 'Use grayscale',
            name: 'featurecounts>display>grayscale',
            type: 'checkbox'
        }
    ],
    label: 'Display',
    name: 'featurecounts>display',
    type: 'submenu',
    isVisible: () => true
};
