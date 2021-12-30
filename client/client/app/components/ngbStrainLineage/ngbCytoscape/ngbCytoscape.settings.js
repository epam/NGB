export default {
    viewer: {},
    style: {
        node: {
            'content': 'data(id)',
            'width': 175,
            'height': 60,
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
            'curve-style': 'straight',
            'width': 1,
            'line-color': '#37474F',
            'target-arrow-color': '#37474F',
            'target-arrow-shape': 'triangle',
            'overlay-color': '#4285F4',
            'overlay-padding': '4px',
            'underlay-color': '#4285F4',
            'underlay-padding': '3px',
            'underlay-opacity': '0',
            'text-wrap': 'wrap'
        },
        edgeLabel: {
            'text-rotation': 'none',
            'target-label': 'data(label)',
            'target-text-offset': '20%',
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
