import {variantsView} from '../modes';

export default {
    displayName: state => {
        return state.variantsView;
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
        }
    ],
    label: 'Variants View',
    name: 'vcf>variantsView',
    type: 'submenu'
};