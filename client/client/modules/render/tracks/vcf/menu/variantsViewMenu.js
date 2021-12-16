import {variantsView} from '../modes';
import {getDivider} from '../../../utilities/menu';

export default {
    displayName: state => {
        return state.variantsView;
    },
    displayAdditionalName: (state, tracks, track) => {
        if (
            tracks.length === 1 &&
            track &&
            track.multisample &&
            state.collapseSamples &&
            state.variantsView === variantsView.variantsViewCollapsed
        ) {
            return 'Collapse samples';
        }
    },
    fields: [
        {
            disable: () => {},
            enable: state => state.variantsView = variantsView.variantsViewCollapsed,
            isEnabled: state => state.variantsView === variantsView.variantsViewCollapsed,
            label: 'Collapsed',
            name: 'vcf>variantsView>collapsed',
            type: 'checkbox'
        },
        {
            disable: () => {},
            enable: state => state.variantsView = variantsView.variantsViewExpanded,
            isEnabled: state => state.variantsView === variantsView.variantsViewExpanded,
            label: 'Expanded',
            name: 'vcf>variantsView>expanded',
            type: 'checkbox'
        },
        getDivider(),
        {
            disable: state => state.collapseSamples = false,
            enable: state => state.collapseSamples = true,
            isEnabled: state => state.collapseSamples,
            disabled: state => state.variantsView === variantsView.variantsViewExpanded,
            label: 'Collapse samples',
            name: 'vcf>variantsView>collapseSamples',
            type: 'checkbox',
            isVisible: (state, tracks, track) => tracks.length === 1 && track.multisample
        }
    ],
    label: 'Variants View',
    name: 'vcf>variantsView',
    type: 'submenu'
};
