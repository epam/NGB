import controller from './ngbStrainLineageNode.controller';

export default  {
    controller: controller.UID,
    template: require('./ngbStrainLineageNode.tpl.html'),
    bindings: {
        nodeDataJson: '@'
    }
};
