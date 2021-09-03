import * as scaleModes from './scaleModes';
import {menu} from '../../../utilities';

/**
 * @param [options] {Object}
 * @param [options.name] {string}
 * @param [options.isVisible] {function(state: object): boolean}
 * @param [options.logScaleVisible] {function(state: object): boolean}
 * @returns {{displayAdditionalName: ((function(*): (string|undefined))|*), displayName: ((function(*): (string|string))|*), name: string, label: string, isVisible: (function(*)), fields: [{disable: (function(*): string), enable: enable, isEnabled: (function(*): boolean), name: string, label: string, type: string}, {disable: (function(*): string), enable: enable, isEnabled: (function(*): boolean), name: string, label: string, type: string}, {disable: disable, enable: enable, isEnabled: (function(*): boolean), name: string, label: string, isVisible: (function(*, *): boolean), type: string}, {name: string, label: string, type: string}, {disable: (function(*): boolean), enable: (function(*): boolean), isEnabled: (function(*): boolean|string|*), name: string, isVisible: (function(): boolean), label: string, type: string}], type: string}}
 */
export default function generateScaleModesMenu (options = {}) {
    const {
        name = 'coverage>scale',
        isVisible = ((state) => state.coverage === undefined || state.coverage),
        logScaleVisible = () => true
    } = options;
    return {
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
                case scaleModes.manualScaleMode:
                    return `Manual${additionalInfo}`;
                case scaleModes.groupAutoScaleMode:
                    return 'Group auto-scale';
                case scaleModes.defaultScaleMode:
                default:
                    return 'Auto-scale';
            }
        },
        fields: [
            {
                disable: state => state.coverageScaleMode = scaleModes.defaultScaleMode,
                enable: (state, tracks, track) => {
                    track.groupAutoScaleManager.unregisterTrack(track);
                    state.coverageScaleMode = scaleModes.defaultScaleMode;
                },
                isEnabled: state => !state.coverageScaleMode ||
                    state.coverageScaleMode === scaleModes.defaultScaleMode,
                label: 'Auto-scale',
                name: `${name}>default`,
                type: 'checkbox'
            },
            {
                // enable & disable methods both sets `scaleModes.manualScaleMode` because
                // they are acting like Perform Button with checkbox
                // (we need to open configuration window first and then set the scale mode:
                // manualScaleMode if we clicked on 'Save' and defaultScaleMode if we clicked
                // on 'Cancel')
                disable: state => state.coverageScaleMode = scaleModes.manualScaleMode,
                enable: () => {
                },
                isEnabled: state => state.coverageScaleMode === scaleModes.manualScaleMode,
                label: 'Manual scale',
                name: `${name}>manual`,
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
                name: `${name}>group-auto-scale`,
                type: 'checkbox',
                isVisible: (state, tracks) => tracks.length > 1
            },
            menu.getDivider(),
            {
                disable: state => state.coverageLogScale = false,
                enable: state => state.coverageLogScale = true,
                isEnabled: state => state.coverageLogScale,
                isVisible: logScaleVisible,
                label: 'Log scale',
                name: `${name}>log`,
                type: 'checkbox'
            }
        ],
        label: 'Scale',
        name,
        type: 'submenu',
        isVisible
    };
}
