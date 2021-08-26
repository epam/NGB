import {displayModes} from '../modes';
import {default as displayModesMenu} from './displayModesMenu';
import {default as generalMenu} from './generalMenu';
import {generateScaleModesMenu, generateStateMutatorFunctions} from '../../common/scaleModes';

const scaleModesMenu = generateScaleModesMenu({
    logScaleVisible: state => state.coverageDisplayMode !== displayModes.heatMapDisplayMode
});

const scaleModesMutators = generateStateMutatorFunctions({
    extraFieldsToCheck: ['coverageDisplayMode'],
    dataTransformFn: track => {
        if (track.cache && track.cache.originalData && track._wigTransformer) {
            track.cache.data = track._wigTransformer.transform(track.cache.originalData, track.viewport);
        }
    },
    dataExtremumFn: track => {
        if (track && track.cache && track.cache.coordinateSystem) {
            return {
                min: track.cache.coordinateSystem.realMinimum,
                max: track.cache.coordinateSystem.realMaximum
            };
        }
        return {};
    }
});

export {scaleModesMutators};
export default [generalMenu, displayModesMenu, scaleModesMenu];
