import {displayModes} from '../modes';

export default {
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
        }
    ],
    label: 'Display',
    name: 'featurecounts>display',
    type: 'submenu',
    isVisible: () => true
};
