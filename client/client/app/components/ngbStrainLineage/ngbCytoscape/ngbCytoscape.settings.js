export default {
    viewer: {},
    style: {
        node: {
            'content': 'data(id)',
            'width': 175,
            'height': 50,
            'text-opacity': 1,
            'text-valign': 'center',
            'text-halign': 'center',
            'shape': 'rectangle',
            'label': 'data(id)',
            'background-opacity': 0,
            'opacity': 0,
            'color': '#000',
            'border-width': 1
        },
        edge: {
            'curve-style': 'bezier',
            'width': 1,
            'line-color': '#37474F',
            'target-arrow-color': '#37474F',
            'target-arrow-shape': 'triangle',
            'overlay-color': '#4285F4',
            'overlay-padding': '4px',
            'underlay-color': '#4285F4',
            'underlay-padding': '3px',
            'underlay-opacity': '0'
        },
        edgeLabel: {
            'text-rotation': 'none',
            'content': 'data(label)',
            'font-size': '12px',
            'font-weight': 'bold',
            'text-background-color': '#fff',
            'text-background-opacity': 1,
            'color': '#2c4f9e'
        }
    },
    defaultLayout: {
        name: 'dagre',
        rankDir: 'TB'
    },
    loadedLayout: {
        name: 'preset'
    },
    options: {
        wheelSensitivity: 0.25,
        minZoom: 0.25,
        maxZoom: 4
    }
};
