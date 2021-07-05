function perform (track, feature) {
    const dispatcher = track.config.dispatcher;
    if (dispatcher) {
        dispatcher.emitSimpleEvent('track:duplicate', {
            track,
            config: track.config,
            state: {
                ...track.state,
                geneFeatures: [feature]
            }
        });
    }
}

export default {
    isVisible: (state, tracks) => tracks.length === 1 && (state.availableFeatures || []).length > 1,
    dynamicFields: (state, tracks, track) => (state.availableFeatures || []).map(featureName => ({
        perform: () => perform(track, featureName),
        label: featureName,
        name: `gene>features>${featureName}`,
        type: 'button',
        hash: () => `gene>features>${featureName}`
    })),
    label: 'Duplicate track',
    name: 'gene>duplicate',
    type: 'submenu'
};
