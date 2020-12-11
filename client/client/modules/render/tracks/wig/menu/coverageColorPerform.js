const isNotSet = a => a === null || a === undefined;
const combineColors = (a, b) => {
    if (isNotSet(a) && !isNotSet(b)) {
        return b;
    }
    if (!isNotSet(a) && isNotSet(b)) {
        return a;
    }
    if (isNotSet(a) && isNotSet(b)) {
        return undefined;
    }
    if (a === b) {
        return a;
    }
    return undefined;
};
function reduce (result, current) {
    const {positive, negative} = result || {};
    const {positive: currentPositive, negative: currentNegative} = current || {};
    return {
        negative: combineColors(negative, currentNegative),
        positive: combineColors(positive, currentPositive)
    };
}

export default function coverageColorPerform (tracks, options) {
    const [dispatcher] = (tracks || [])
        .map(track => track.config.dispatcher)
        .filter(Boolean);
    if (dispatcher) {
        const payload = (tracks || []).map(track => {
            const defaultWigColors = track.trackConfig.wig || {};
            const wigSettings = {
                ...track.trackConfig.wig,
                ...track.state.wigColors
            };
            return {
                defaults: {
                    negative: defaultWigColors.negative,
                    positive: defaultWigColors.positive,
                },
                settings: {
                    negative: wigSettings.negative,
                    positive: wigSettings.positive,
                }
            };
        });
        const defaults = payload.map(p => p.defaults);
        const settings = payload.map(p => p.settings);
        const config = {
            defaults: defaults.reduce(reduce, {}),
            settings: settings.reduce(reduce, {}),
        };
        dispatcher.emitSimpleEvent('wig:color:configure', {
            config,
            options,
            sources: (tracks || []).map(track => track.config.name)
        });
    }
}