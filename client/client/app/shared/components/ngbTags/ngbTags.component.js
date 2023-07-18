import controller from './ngbTags.controller';

export default {
    bindings: {
        title: '<',
        tags: '=',
        editable: '<',
        addLabel: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbTags.tpl.html')
};
