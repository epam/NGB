export default {
    viewer: {},
    style: {
        node: {
            'text-opacity': 1,
            'text-valign': 'center',
            'text-halign': 'center',
            'shape': 'rectangle',
            'color': '#000',
            'border-width': 1
        }
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
        minZoom: 0.25,
        maxZoom: 4
    }
};
