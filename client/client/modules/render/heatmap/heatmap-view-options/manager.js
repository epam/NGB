const LOCAL_STORAGE_KEY = 'heatmaps-state';

function readStates() {
    try {
        return JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY));
    } catch (_) {
        return {};
    }
}

function writeStates(states) {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(states || {}));
}

export function readHeatmapState(heatmapId) {
    const states = readStates() || {};
    return states[heatmapId];
}

export function writeHeatmapState(heatmapId, state = undefined) {
    const states = readStates() || {};
    if (state) {
        states[heatmapId] = state;
    } else if (Object.prototype.hasOwnProperty.call(states, heatmapId)) {
        delete states[heatmapId];
    }
    writeStates(states);
}
