function resizePerform (tracks, options) {
    const [dispatcher] = (tracks || [])
        .map(track => track.config.dispatcher)
        .filter(Boolean);
    if (dispatcher) {
        dispatcher.emitSimpleEvent('tracks:height:configure', {
            options,
            sources: (tracks || []).map(track => track.config.name),
            tracks,
            types: [...(new Set((tracks || []).map(track => track.config.format)))]
        });
    }
}

export default {
    common: true,
    exclude: ['REFERENCE'],
    label: 'Resize',
    name: 'general>resize',
    perform: resizePerform,
    type: 'button'
};
