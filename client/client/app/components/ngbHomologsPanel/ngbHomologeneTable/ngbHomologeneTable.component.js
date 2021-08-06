import controller from './ngbHomologeneTable.controller';

export default {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbHomologeneTable.tpl.html'),
};
