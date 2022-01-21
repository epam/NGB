import downloadFile from '../../common/menu/downloadFile';
import header from '../../common/menu/header';
import {menu} from '../../../utilities';

export default {
    displayName: state => {
        const selectedStates = [];
        if (state.referenceShowForwardStrand) {
            selectedStates.push('Forward strand');
        }
        if (state.referenceShowReverseStrand)  {
            selectedStates.push('Reverse strand');
        }
        if (state.referenceShowTranslation)  {
            selectedStates.push('Translation');
        }
        if(selectedStates.length > 0) {
            return `General (${selectedStates.join(', ')})`;
        } else {
            return 'General';
        }
    },
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
        },
        menu.getDivider(),
        {
            label: 'Motifs search',
            name: 'reference>searchMotifs',
            type: 'button',
            perform: (tracks) => {
                const [dispatcher] = (tracks || [])
                    .map(track => track.config.dispatcher)
                    .filter(Boolean);
                if (dispatcher) {
                    dispatcher.emitSimpleEvent('search:motifs:open');
                }
            }
        },
        menu.getDivider(),
        header,
        downloadFile
    ],
    name: 'reference>general',
    type: 'submenu'
};
