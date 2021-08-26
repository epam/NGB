function sourceIsEnabled (state, source) {
    return !state.selectedSources || state.selectedSources.indexOf(source) >= 0;
}

function menuIsDisabled (state, source) {
    return sourceIsEnabled(state, source) && (
        (state.sources || []).length <= 1 ||
        (state.selectedSources || []).length === 1
    );
}

function enableSource (state, source, track) {
    const selectedSources = (state.selectedSources || []).slice();
    state.selectedSources = [...selectedSources, source];
    if (track && track._flags) {
        track._flags.dataChanged = true;
        track.reportTrackState();
        if (typeof track.sourceTypesChanged === 'function') {
            track.sourceTypesChanged();
        }
    }
}

function disableSource (state, source, track) {
    let selectedSources = state.selectedSources;
    if (!selectedSources || !selectedSources.length) {
        selectedSources = (state.sources || []).slice();
    }
    state.selectedSources = selectedSources.filter(sourceName => sourceName !== source);
    if (track && track._flags) {
        track._flags.dataChanged = true;
        track.reportTrackState();
        if (typeof track.sourceTypesChanged === 'function') {
            track.sourceTypesChanged();
        }
    }
}

function getDisplayName (state) {
    if (
        !state.selectedSources ||
        !state.sources
    ) {
        return 'all';
    }
    const filtered = state.selectedSources.filter(feature => state.sources.indexOf(feature) >= 0);
    if (filtered.length === state.sources.length) {
        return 'all';
    }
    if (filtered.length > 1) {
        const length = filtered.length;
        return `${length} source${length > 1 ? 's' : ''}`;
    }
    return filtered.join(', ');
}

export default {
    displayName: state => getDisplayName(state),
    label: 'Sources',
    name: 'featurecounts>sources',
    type: 'submenu',
    isVisible: (state, tracks) => tracks.length === 1 && state.sources && state.sources.length > 1,
    preventAutoClose: true,
    dynamicFields: (state, tracks, track) => (state.sources || []).map(source => ({
        disable: () => disableSource(state, source, track),
        enable: () => enableSource(state, source, track),
        disabled: () => menuIsDisabled(state, source),
        isEnabled: () => sourceIsEnabled(state, source),
        label: source,
        name: `featurecounts>sources>${source}`,
        type: 'checkbox',
        hash: () => `featurecounts>sources>${source}`
    })),
};
