import {generateScaleModesMenu, generateStateMutatorFunctions} from '../../common/scaleModes';
import {displayModes} from '../modes';

const scaleModesMenuName = 'featurecounts>scale';

const scaleModesMenu = generateScaleModesMenu({
    name: scaleModesMenuName,
    isVisible: state => state.featureCountsDisplayMode === displayModes.barChart,
    logScaleVisible: state => state.featureCountsDisplayMode === displayModes.barChart
});

const scaleModesStateMutators = generateStateMutatorFunctions({
    scaleModesMenuName,
    extraFieldsToCheck: ['featureCountsDisplayMode'],
    disableGroupAutoScaleCriteria: (stateBefore, stateNow) => stateBefore &&
        stateNow &&
        stateBefore.featureCountsDisplayMode !== stateNow.featureCountsDisplayMode &&
        stateNow.featureCountsDisplayMode !== displayModes.barChart,
    dataExtremumFn: track => {
        if (track && track.cache && track.cache.sources) {
            const values = Object.values(track.cache.sources.values || {})
                .map(o => o.data || [])
                .reduce((r, c) => ([...r, ...c.map(o => o.value)]), []);
            const min = Math.min(...values);
            const max = Math.max(...values);
            return {
                min,
                max
            };
        }
        return {};
    }
});

export {scaleModesStateMutators};
export default scaleModesMenu;
