export default function coverageColorPerform (tracks) {
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
                config: {
                    defaults: {
                        negative: defaultWigColors.negative,
                        positive: defaultWigColors.positive,
                    },
                    settings: {
                        negative: wigSettings.negative,
                        positive: wigSettings.positive,
                    },
                },
                source: track.config.name,
            };
        });
        dispatcher.emitSimpleEvent('wig:color:configure', payload);
    }
}