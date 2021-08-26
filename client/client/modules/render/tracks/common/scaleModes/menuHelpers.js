import * as scaleModes from './scaleModes';

function preStateMutatorFn (fieldsToCheck = []) {
    return (track) => ({
        currentScaleMode: track.state.coverageScaleMode,
        logScaleEnabled: track.state.coverageLogScale,
        ...fieldsToCheck
            .map(field => ({[field]: track.state[field]}))
            .reduce((r, c) => ({...r, ...c}), {})
    });
}

function postStateMutatorFn (
    scaleModesMenuName,
    transformDataOnGroupAutoScale,
    fieldsToCheck = [],
    disableGroupAutoScaleCriteria
) {
    return (track, key, prePayload) => {
        const {
            currentScaleMode,
            logScaleEnabled
        } = prePayload || {};
        let shouldReportTrackState = true;
        if (key === `${scaleModesMenuName}>manual`) {
            shouldReportTrackState = false;
        } else if (key === `${scaleModesMenuName}>group-auto-scale`) {
            shouldReportTrackState = true;
            if (typeof transformDataOnGroupAutoScale === 'function') {
                transformDataOnGroupAutoScale(track);
            }
        } else if (currentScaleMode !== track.state.coverageScaleMode) {
            track._flags.dataChanged = true;
            track.state.coverageScaleFrom = undefined;
            track.state.coverageScaleTo = undefined;
            shouldReportTrackState = currentScaleMode === scaleModes.groupAutoScaleMode;
            track._flags.dataChanged = true;
        } else if (logScaleEnabled !== track.state.coverageLogScale) {
            track._flags.dataChanged = true;
        } else if (fieldsToCheck.filter(field => prePayload[field] !== track.state[field]).length > 0) {
            track._flags.dataChanged = true;
        }
        if (
            track.state.coverageScaleMode === scaleModes.groupAutoScaleMode &&
            typeof disableGroupAutoScaleCriteria === 'function' &&
            disableGroupAutoScaleCriteria(prePayload, track.state) &&
            track.groupAutoScaleManager
        ) {
            track.groupAutoScaleManager.unregisterTrack(track);
            track.state.coverageScaleMode = scaleModes.defaultScaleMode;
        }
        return shouldReportTrackState;
    };
}

function getCoverageExtremum (track, dataExtremumFn) {
    let max = track.state.coverageScaleTo;
    let min = track.state.coverageScaleFrom;
    const realValues = dataExtremumFn ? dataExtremumFn(track) : {};
    const isNone = o => o === undefined || o === null;
    if (isNone(max) && realValues.max !== undefined) {
        max = realValues.max;
    }
    if (isNone(min) && realValues.min !== undefined) {
        min = realValues.min;
    }
    return {max, min};
}

function generateTracksCoverageExtremumFn (tracks, dataExtremumFn) {
    return function () {
        const values = (tracks || []).map(track => getCoverageExtremum(track, dataExtremumFn));
        return values.reduce((r, c) => ({
            max: Math.min(c.max, r.max),
            min: Math.max(c.min, r.min)
        }), {max: Infinity, min: -Infinity});
    };
}

function afterStateMutatorFn (
    scaleModesMenuName,
    configureManualScaleEventName,
    dataExtremumFn
) {
    return (tracks, key) => {
        if (key === `${scaleModesMenuName}>manual`) {
            const formats = [...(new Set(tracks.map(track => track.config.format)))];
            const getTracksCoverageExtremum = generateTracksCoverageExtremumFn(tracks, dataExtremumFn);
            const isLogScale = (tracks || [])
                .map(track => track.state.coverageLogScale)
                .reduce((r, c) => r && c, true);
            const [dispatcher] = (tracks || [])
                .map(track => track.config.dispatcher)
                .filter(Boolean);
            const [browserId] = (tracks || [])
                .map(track => track.config.browserId)
                .filter(Boolean);
            if (dispatcher && configureManualScaleEventName) {
                dispatcher.emitSimpleEvent(configureManualScaleEventName, {
                    config: {
                        extremumFn: getTracksCoverageExtremum,
                        isLogScale
                    },
                    options: {
                        browserId,
                        group: (tracks || []).length > 1,
                        formats
                    },
                    sources: (tracks || []).map(track => track.config.name),
                });
            }
        }
    };
}

/**
 * @param [options] {Object}
 * @param [options.extraFieldsToCheck] {Array<string>}
 * @param [options.disableGroupAutoScaleCriteria] {function: boolean}
 * @param [options.scaleModesMenuName] {string}
 * @param [options.dataTransformFn] {function}
 * @param [options.configureManualScaleEventName] {string}
 * @param [options.dataExtremumFn] {function(track): {min: number, max: number}}
 * @returns {{postStateMutatorFn: function, preStateMutatorFn: function, afterStateMutatorFn: function}}
 */
export default function generateStateMutatorHelpers (options = {}) {
    const {
        extraFieldsToCheck = [],
        disableGroupAutoScaleCriteria,
        scaleModesMenuName = 'coverage>scale',
        dataTransformFn,
        configureManualScaleEventName = 'tracks:coverage:manual:configure',
        dataExtremumFn
    } = options;
    const preStateMutator = preStateMutatorFn(extraFieldsToCheck);
    const postStateMutator = postStateMutatorFn(
        scaleModesMenuName,
        dataTransformFn,
        extraFieldsToCheck,
        disableGroupAutoScaleCriteria
    );
    const afterStateMutator = afterStateMutatorFn(
        scaleModesMenuName,
        configureManualScaleEventName,
        dataExtremumFn
    );
    return {
        preStateMutatorFn: preStateMutator,
        postStateMutatorFn: postStateMutator,
        afterStateMutatorFn: afterStateMutator
    };
}
