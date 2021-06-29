function featureIsEnabled (state, feature) {
    return !state.geneFeatures || state.geneFeatures.indexOf(feature) >= 0;
}

function menuIsDisabled (state, feature) {
    return featureIsEnabled(state, feature) && (
        (state.availableFeatures || []).length <= 1 ||
        (state.geneFeatures || []).length === 1
    );
}

function enableFeature (state, feature, track) {
    const geneFeatures = (state.geneFeatures || []).slice();
    state.geneFeatures = [...geneFeatures, feature];
    if (track && track._flags) {
        track._flags.dataChanged = true;
        track.reportTrackState();
    }
}

function disableFeature (state, feature, track) {
    let geneFeatures = state.geneFeatures;
    if (!geneFeatures || !geneFeatures.length) {
        geneFeatures = (state.availableFeatures || []).slice();
    }
    state.geneFeatures = geneFeatures.filter(geneFeature => geneFeature !== feature);
    if (track && track._flags) {
        track._flags.dataChanged = true;
        track.reportTrackState();
    }
}

function getDisplayName (state) {
    if (
        !state.geneFeatures ||
        !state.availableFeatures
    ) {
        return 'all';
    }
    const filtered = state.geneFeatures.filter(feature => state.availableFeatures.indexOf(feature) >= 0);
    if (filtered.length === state.availableFeatures.length) {
        return 'all';
    }
    if (filtered.length > 3) {
        const length = filtered.length;
        return `${length} type${length > 1 ? 's' : ''}`;
    }
    return filtered.join(', ');
}

export default {
    displayName: state => getDisplayName(state),
    isVisible: (state) => (state.availableFeatures || []).length > 1,
    dynamicFields: (state, tracks, track) => (state.availableFeatures || []).map(featureName => ({
        disable: () => disableFeature(state, featureName, track),
        enable: () => enableFeature(state, featureName, track),
        disabled: () => menuIsDisabled(state, featureName),
        isEnabled: () => featureIsEnabled(state, featureName),
        label: featureName,
        name: `gene>features>${featureName}`,
        type: 'checkbox',
        hash: () => `gene>features>${featureName}`
    })),
    label: 'Features',
    name: 'gene>features',
    type: 'submenu',
    preventAutoClose: true,
    capitalized: false
};
