import {menu} from '../../../utilities';

export default {
    fields: [
        {
            disable: state => state.referenceShowTranslation = false,
            enable: state => state.referenceShowTranslation = true,
            isEnabled: state => state.referenceShowTranslation,
            label: 'Show translation',
            name: 'reference>showTranslation',
            type: 'checkbox'
        },
        menu.getDivider(),
        {
            disable: state => state.referenceShowForwardStrand = false,
            enable: state => state.referenceShowForwardStrand = true,
            isEnabled: state => state.referenceShowForwardStrand,
            label: 'Show forward strand',
            name: 'reference>showForwardStrand',
            type: 'checkbox'
        },
        {
            disable: state => state.referenceShowReverseStrand = false,
            enable: state => state.referenceShowReverseStrand = true,
            isEnabled: state => state.referenceShowReverseStrand,
            label: 'Show reverse strand',
            name: 'reference>showReverseStrand',
            type: 'checkbox'
        }
    ],
    label: 'General',
    name: 'reference>general',
    type: 'submenu'
};