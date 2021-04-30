function headerPerform (tracks, options) {
    const [dispatcher] = (tracks || [])
        .map(track => track.config.dispatcher)
        .filter(Boolean);
    function reduce (result, current) {
        const {fontSize} = result || {};
        const {fontSize: currentFontSize} = current || {};
        if (!result && !fontSize) {
            return {
                fontSize: currentFontSize
            };
        } else if (!currentFontSize) {
            return {
                fontSize: fontSize
            };
        } else if (fontSize === currentFontSize) {
            return {
                fontSize
            };
        }
        return {
            fontSize: undefined
        };
    }
    if (dispatcher) {
        const configHeaders = (tracks || []).map(track => track.config.header);
        const stateHeaders = (tracks || []).map(track => track.state.header);
        dispatcher.emitSimpleEvent('tracks:header:style:configure', {
            config: {
                defaults: {...configHeaders.reduce(reduce)},
                settings: {
                    ...configHeaders.reduce(reduce),
                    ...stateHeaders.reduce(reduce)}
            },
            options,
            sources: (tracks || []).map(track => track.config.name),
            types: [...(Array.from(new Set((tracks || []).map(track => track.config.format))))]
        });
    }
}

export default {
    common: true,
    label: 'Font size',
    name: 'general>font',
    perform: headerPerform,
    type: 'button'
};
