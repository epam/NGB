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
            'border-width': 1
        },
        edge: {
            'curve-style': 'taxi',
            'taxi-turn': 20,
            'taxi-turn-min-distance': 5,
            'width': 3,
            'line-color': 'grey',
            'target-arrow-color': 'grey',
            'target-arrow-shape': 'triangle',
        },
        edgeLabel: {
            'text-rotation': 'none',
            'content': 'data(label)',
            'font-size': '12px',
            'font-weight': 'bold',
            'text-background-color': '#fff',
            'text-background-opacity': 1
        }
    },
    defaultLayout: {
        name: 'dagre',
        rankDir: 'TB'
    },
    loadedLayout: {
        name: 'preset'
    },
    options: {}
};
