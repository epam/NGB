export default {
    viewer: {},
    style: {
        node: {
            'content': 'data(id)',
            'text-opacity': 0,
            'text-valign': 'center',
            'text-halign': 'center',
            'label': 'data(id)',
            'background-opacity': 0,
            'background-image-opacity': 0,
            'background-repeat': 'no-repeat',
            'color': '#000',
            'border-color': '#000',
            'opacity': 0,
            'border-width': 0,
            'padding': 0
        },
    },
    defaultLayout: {
        name: 'dagre',
        rankDir: 'TB'
    },
    loadedLayout: {
        name: 'preset',
        nodeDimensionsIncludeLabels: true,
    },
    options: {
        wheelSensitivity: 0.25,
        minZoom: 0.05,
        maxZoom: 4
    },
    externalOptions: {
        zoomStep: 0.05
    }
};
