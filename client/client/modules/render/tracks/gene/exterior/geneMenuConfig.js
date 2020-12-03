import * as geneTypes from '../geneTypes';
import {menu} from '../../../utilities';

export default [
    {
        fields:[
            menu.getActionButton(
              'coverage>font',
              'Font size',
              (config, dispatcher, state) => {
                  dispatcher.emitSimpleEvent('tracks:header:style:configure', {
                      config: {
                          defaults: config.header,
                          settings: {
                              ...config.header,
                              ...state.header
                          },
                      },
                      source: config.name,
                  });
              }
            ),
        ],
        label:'General',
        name:'gene>general',
        type: 'submenu',
    },
    {
        displayName: state => state.geneTranscript,
        fields: [
            {
                disable: state => state.geneTranscript = geneTypes.transcriptViewTypes.expanded,
                enable: state => state.geneTranscript = geneTypes.transcriptViewTypes.collapsed,
                isEnabled: state => state.geneTranscript === geneTypes.transcriptViewTypes.collapsed,
                label: 'Collapsed',
                name: 'gene>transcript>collapsed',
                type: 'checkbox'
            },
            {
                disable: state => state.geneTranscript = geneTypes.transcriptViewTypes.collapsed,
                enable: state => state.geneTranscript = geneTypes.transcriptViewTypes.expanded,
                isEnabled: state => state.geneTranscript === geneTypes.transcriptViewTypes.expanded,
                label: 'Expanded',
                name: 'gene>transcript>expanded',
                type: 'checkbox'
            }
        ],
        label: 'Transcript View',
        name: 'gene>view',
        type: 'submenu'
    }
];
