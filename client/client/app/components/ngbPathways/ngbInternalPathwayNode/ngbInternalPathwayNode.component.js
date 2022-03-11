import controller from './ngbInternalPathwayNode.controller';

export default  {
    controller: controller.UID,
    template: require('./ngbInternalPathwayNode.tpl.html'),
    bindings: {
        nodeDataJson: '@',
        onElementClick: '&'
    }
};
