import controller from './ngbTag.controller';

export default {
    bindings: {
        tag: '<',
        removable: '<',
        onRemove: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbTag.tpl.html')
};
