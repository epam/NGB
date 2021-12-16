import {variantsView} from '../modes';
import {getDivider} from '../../../utilities/menu';

export default {
    displayName: state => {
        return state.variantsView;
    },
    displayAdditionalName: (state, tracks, track) => {
        const parts = [];
        if (
            tracks.length === 1 &&
            track &&
            track.multisample &&
            state.variantsDensity
        ) {
            parts.push('Show density');
        }
        if (
            tracks.length === 1 &&
            track &&
            track.multisample &&
            state.collapseSamples &&
            state.variantsView === variantsView.variantsViewCollapsed
        ) {
            parts.push('Merge samples');
        }
        if (parts.length > 0) {
            return parts.join('/');
        }
        return undefined;
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
            label: 'Merge samples',
            name: 'vcf>variantsView>collapseSamples',
            type: 'checkbox',
            isVisible: (state, tracks, track) => tracks.length === 1 && track.multisample
        },
        getDivider(),
        {
            disable: state => state.variantsDensity = false,
            enable: state => state.variantsDensity = true,
            isEnabled: state => state.variantsDensity,
            label: 'Show density',
            name: 'vcf>variantsDensity',
            type: 'checkbox',
            isVisible: (state, tracks, track) => tracks.length === 1 && track.multisample
        }
    ],
    label: 'Variants View',
    name: 'vcf>variantsView',
    type: 'submenu'
};
