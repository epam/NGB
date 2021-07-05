import {displayModes, scaleModes} from '../modes';
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
                return 'Auto-scale';
            case scaleModes.manualScaleMode:
                return `Manual${additionalInfo}`;
            case scaleModes.groupAutoScaleMode:
                return 'Group auto-scale';
        }
    },
    fields: [
        {
            disable: state => state.coverageScaleMode = scaleModes.defaultScaleMode,
            enable: (state, tracks, track) => {
                track.groupAutoScaleManager.unregisterTrack(track);
                state.coverageScaleMode = scaleModes.defaultScaleMode;
            },
            isEnabled: state => state.coverageScaleMode === scaleModes.defaultScaleMode,
            label: 'Auto-scale',
            name: 'coverage>scale>default',
            type: 'checkbox'
        },
        {
            // enable & disable methods both sets `scaleModes.manualScaleMode` because
            // they are acting like Perform Button with checkbox
            // (we need to open configuration window first and then set the scale mode:
            // manualScaleMode if we clicked on 'Save' and defaultScaleMode if we clicked
            // on 'Cancel')
            disable: state => state.coverageScaleMode = scaleModes.manualScaleMode,
            enable: () => {},
            isEnabled: state => state.coverageScaleMode === scaleModes.manualScaleMode,
            label: 'Manual scale',
            name: 'coverage>scale>manual',
            type: 'checkbox'
        },
        {
            disable: (state, tracks, track) => {
                track.groupAutoScaleManager.unregisterTrack(track);
                state.coverageScaleMode = scaleModes.defaultScaleMode;
            },
            enable: (state, tracks, track) => {
                const groupAutoScale = [
                    track.config.browserId || 'default',
                    ...tracks.map(t => `${t.config.bioDataItemId.toString()}/${t.config.duplicateId || ''}`)
                ].join('-');
                if (
                    state.coverageScaleMode === scaleModes.groupAutoScaleMode &&
                    groupAutoScale !== state.groupAutoScale
                ) {
                    track.groupAutoScaleManager.unregisterTrack(track);
                }
                state.groupAutoScale = groupAutoScale;
                state.coverageScaleMode = scaleModes.groupAutoScaleMode;
                track._flags.dataChanged = true;
            },
            isEnabled: state => state.coverageScaleMode === scaleModes.groupAutoScaleMode,
            label: 'Group Auto-scale',
            name: 'coverage>scale>group-auto-scale',
            type: 'checkbox',
            isVisible: (state, tracks) => tracks.length > 1
        },
        menu.getDivider(),
        {
            disable: state => state.coverageLogScale = false,
            enable: state => state.coverageLogScale = true,
            isEnabled: state => state.coverageLogScale,
            isVisible: state => state.coverageDisplayMode !== displayModes.heatMapDisplayMode,
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
