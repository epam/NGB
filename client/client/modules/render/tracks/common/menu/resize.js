function resizePerform (tracks) {
    const [dispatcher] = (tracks || [])
        .map(track => track.config.dispatcher)
        .filter(Boolean);
    if (dispatcher) {
        dispatcher.emitSimpleEvent('tracks:height:configure', tracks);
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
