import ngbCytoscapeController from './ngbCytoscape.controller';

export default  {
    template: require('./ngbCytoscape.tpl.html'),
    bindings: {
        elements: '<'
    },
    controller: ngbCytoscapeController
};
