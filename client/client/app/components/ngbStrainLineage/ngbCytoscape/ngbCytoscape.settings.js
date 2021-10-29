export default {
    viewer: {},
    style: {
        node: {
            'content': 'data(id)',
            'width': 100,
            'height': 50,
            'text-opacity': 1,
            'text-valign': 'center',
            'text-halign': 'center',
            'shape': 'rectangle',
            'label': 'data(id)',
            'background-opacity': 0,
            'opacity': 0,
            'color': '#000',
            'border-width': 1,
            'border-color': '#ccc'
        },
        edge: {
            'curve-style': 'taxi',
            'taxi-turn': 20,
            'taxi-turn-min-distance': 5,
            'width': 3,
            'target-arrow-color': '#ccc',
            'target-arrow-shape': 'triangle'
        }
    },
    defaultLayout: {
        name: 'dagre',
        rankDir: 'TB'
    },
    options: {
    }
};
