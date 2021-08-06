import controller from './ngbHomologeneResultTable.controller';

export default {
    bindings: {
        changeState: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbHomologeneResultTable.tpl.html'),
};
