import controller from './ngbOrthoParaTable.controller';

export default {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbOrthoParaTable.tpl.html'),
};
