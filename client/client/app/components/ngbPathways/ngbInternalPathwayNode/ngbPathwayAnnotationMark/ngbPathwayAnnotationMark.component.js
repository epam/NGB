import controller from './ngbPathwayAnnotationMark.controller';

export default  {
    controller: controller.UID,
    template: require('./ngbPathwayAnnotationMark.tpl.html'),
    bindings: {
        annotations: '<'
    }
};
