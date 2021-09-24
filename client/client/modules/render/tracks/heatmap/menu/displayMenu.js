function setDendrogramMode(state, mode) {
    if (state && state.heatmap) {
        state.heatmap.dendrogram = mode;
    }
}

export default {
    displayName: state => {
        if (state && state.heatmap && state.heatmap.dendrogram) {
            return 'Dendrogram';
        }
        return undefined;
    },
    fields: [
        {
            disable: state => setDendrogramMode(state, false),
            enable: state => setDendrogramMode(state, true),
            isEnabled: state => state && state.heatmap && state.heatmap.dendrogram,
            label: 'Dendrogram',
            name: 'heatmap>display>dendrogram',
            type: 'checkbox',
            disabled: state => !(state && state.heatmap && state.heatmap.dendrogramAvailable)
        }
    ],
    label: 'Display',
    name: 'heatmap>display',
    type: 'submenu',
    isVisible: (state, tracks) => tracks.length === 1 &&
        (state && state.heatmap && state.heatmap.dendrogramAvailable)
};
