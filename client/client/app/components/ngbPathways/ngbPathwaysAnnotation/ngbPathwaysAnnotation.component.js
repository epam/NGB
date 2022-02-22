import controller from './ngbPathwaysAnnotation.controller';

export default {
    bindings: {
        annotation: '='
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbPathwaysAnnotation.tpl.html'),
};
