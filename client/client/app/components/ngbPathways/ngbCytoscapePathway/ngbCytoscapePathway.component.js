import ngbCytoscapePathwayController from './ngbCytoscapePathway.controller';

export default  {
    template: require('./ngbCytoscapePathway.tpl.html'),
    bindings: {
        elements: '<',
        tag: '@',
        onElementClick: '&',
        storageName: '@',
        elementsOptions: '<'
    },
    controller: ngbCytoscapePathwayController
};
