import {displayModes, scaleModes} from '../../wig/modes';
import {default as coverageColorPerform} from '../../wig/menu/coverageColorPerform';
import {default as displayModesMenu} from '../../wig/menu/displayModesMenu';
import {getDivider} from '../../../utilities/menu';
import {default as scaleModesMenu} from '../../wig/menu/scaleModesMenu';

export default {
    displayName: state => {
        const parts = [];
        switch (state.coverageDisplayMode) {
            case displayModes.heatMapDisplayMode:
                parts.push('Heat Map');
                break;
            default:
            case displayModes.defaultDisplayMode:
                parts.push('Bar Graph');
                break;
        }
        switch (state.coverageScaleMode) {
            case scaleModes.defaultScaleMode:
                parts.push('Auto-scale');
                break;
            case scaleModes.manualScaleMode: {
                let additionalInfo = '';
                if (state.coverageScaleFrom !== undefined && state.coverageScaleTo !== undefined) {
                    additionalInfo = ` (${state.coverageScaleFrom} - ${state.coverageScaleTo})`;
                }
                parts.push(`Manual${additionalInfo}`);
            }
                break;
            case scaleModes.groupAutoScaleMode:
                parts.push('Group auto-scale');
                break;
        }
        return parts.join('/');
    },
    fields: [
        ...displayModesMenu.fields,
        getDivider(),
        ...scaleModesMenu.fields.filter(f => f.type !== 'divider'),
        getDivider(),
        {
            label: 'Color',
            name: 'coverage>color',
            perform: coverageColorPerform,
            type: 'button'
        }
    ],
    label: 'Coverage',
    name: 'bam>coverage',
    type: 'submenu',
    isVisible: state => state.coverage
};
