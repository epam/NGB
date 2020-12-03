export default function coverageResizePerform (tracks) {
    const [dispatcher] = (tracks || [])
        .map(track => track.config.dispatcher)
        .filter(Boolean);
    if (dispatcher) {
        dispatcher.emitSimpleEvent('wig:height:configure', tracks);
    }
}