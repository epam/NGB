import controller from './ngbTags.controller';

export default {
    bindings: {
        tagTitle: '<',
        tags: '=',
        editable: '<',
        addLabel: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbTags.tpl.html')
};
