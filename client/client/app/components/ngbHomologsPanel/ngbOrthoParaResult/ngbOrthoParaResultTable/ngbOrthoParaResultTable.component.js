import controller from './ngbOrthoParaResultTable.controller';

export default {
    bindings: {
        changeState: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbOrthoParaResultTable.tpl.html'),
};
