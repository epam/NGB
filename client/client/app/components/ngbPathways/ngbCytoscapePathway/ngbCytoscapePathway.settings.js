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
            'overlay-padding': '6px',
            'underlay-color': '#000',
            'underlay-opacity': 0,
            'opacity': 0,
            'border-width': 0,
            'padding': 0
        },
        internalEdge: {
            'line-color': '#37474F',
            'target-arrow-color': '#37474F',
            'overlay-color': '#4285F4',
            'overlay-padding': '4px',
            'underlay-color': '#4285F4',
            'underlay-padding': '3px',
            'underlay-opacity': '0'
        },
        collageEdge: {
            'line-color': '#178592',
            'target-arrow-color': '#178592',
            'overlay-color': '#4285F4',
            'overlay-padding': '6px',
            'underlay-color': '#000',
            'underlay-padding': '3px',
            'underlay-opacity': 0
        },
        collageSelected: {
            'overlay-color': '#000',
            'overlay-opacity': 0.1
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
        minZoom: 0.05,
        maxZoom: 4
    },
    externalOptions: {
        zoomStep: 0.05
    }
};
