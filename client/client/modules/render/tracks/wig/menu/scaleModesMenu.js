import scaleModes from '../modes';
import {menu} from '../../../utilities';

export default {
    displayAdditionalName: state => {
        if (state.coverageLogScale) {
            return 'Log scale';
        }
    },
    displayName: state => {
        let additionalInfo = '';
        if (state.coverageScaleFrom !== undefined && state.coverageScaleTo !== undefined) {
            additionalInfo = ` (${state.coverageScaleFrom} - ${state.coverageScaleTo})`;
        }
        switch (state.coverageScaleMode) {
            case scaleModes.defaultScaleMode:
                return 'Default';
            case scaleModes.manualScaleMode:
                return `Manual${additionalInfo}`;
        }
    },
    fields: [
        {
            disable: state => state.coverageScaleMode = scaleModes.defaultScaleMode,
            enable: state => state.coverageScaleMode = scaleModes.defaultScaleMode,
            isEnabled: state => state.coverageScaleMode === scaleModes.defaultScaleMode,
            label: 'Default scale',
            name: 'coverage>scale>default',
            type: 'checkbox'
        },
        {
            disable: state => state.coverageScaleMode = scaleModes.manualScaleMode,
            enable: state => state.coverageScaleMode = scaleModes.manualScaleMode,
            isEnabled: state => state.coverageScaleMode === scaleModes.manualScaleMode,
            label: 'Manual scale',
            name: 'coverage>scale>manual',
            type: 'checkbox'
        },
        menu.getDivider(),
        {
            disable: state => state.coverageLogScale = false,
            enable: state => state.coverageLogScale = true,
            isEnabled: state => state.coverageLogScale,
            label: 'Log scale',
            name: 'coverage>scale>log',
            type: 'checkbox'
        }
    ],
    label: 'Scale',
    name: 'coverage>scale',
    type: 'submenu',
    isVisible: state => state.coverage === undefined || state.coverage
};