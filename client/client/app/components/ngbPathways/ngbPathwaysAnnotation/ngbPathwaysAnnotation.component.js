import controller from './ngbPathwaysAnnotation.controller';

export default {
    bindings: {
        annotation: '=',
        colorScheme: '='
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbPathwaysAnnotation.tpl.html'),
};
