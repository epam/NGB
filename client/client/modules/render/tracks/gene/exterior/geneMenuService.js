import * as geneTypes from '../geneTypes';
import geneMenuConfig from './geneMenuConfig';

export default class GeneMenuService {
    constructor() {

    }

    actions = {
        'gene>transcript>collapsed': {
            disable: state => state.geneTranscript = geneTypes.transcriptViewTypes.expanded,
            enable: state => state.geneTranscript = geneTypes.transcriptViewTypes.collapsed,
            isEnabled: state => state.geneTranscript === geneTypes.transcriptViewTypes.collapsed
        },
        'gene>transcript>expanded': {
            disable: state => state.geneTranscript = geneTypes.transcriptViewTypes.collapsed,
            enable: state => state.geneTranscript = geneTypes.transcriptViewTypes.expanded,
            isEnabled: state => state.geneTranscript === geneTypes.transcriptViewTypes.expanded
        }
    };

    getMenu() {
        geneMenuConfig && geneMenuConfig.forEach(submenu=> {
            submenu.fields.forEach(field=> {
                const actions = this.actions[field.name];
                Object.assign(field, actions);
            });
        });
        return geneMenuConfig;
    }

}

