function setDendrogramMode(state, mode) {
    if (state && state.heatmap) {
        state.heatmap.dendrogram = mode;
    }
}

function setAnnotationsMode(state, mode) {
    if (state && state.heatmap) {
        state.heatmap.annotations = mode;
    }
}

export default {
    displayName: state => {
        const parts = [];
        if (state && state.heatmap && state.heatmap.annotations) {
            parts.push('Annotations');
        }
        if (state && state.heatmap && state.heatmap.dendrogram) {
            parts.push('Dendrogram');
        }
        return parts.length > 0 ? parts.join(', ') : undefined;
    },
    fields: [
        {
            disable: state => setAnnotationsMode(state, false),
            enable: state => setAnnotationsMode(state, true),
            isEnabled: state => state && state.heatmap && state.heatmap.annotations,
            label: 'Row/column annotations',
            name: 'heatmap>display>annotations',
            type: 'checkbox',
            disabled: state => !(state && state.heatmap && state.heatmap.annotationsAvailable)
        },
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
        state &&
        state.heatmap &&
        (state.heatmap.dendrogramAvailable || state.heatmap.annotationsAvailable)
};
