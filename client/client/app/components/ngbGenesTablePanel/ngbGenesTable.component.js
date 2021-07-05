import controller from './ngbGenesTable.controller';

export default {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbGenesTable.tpl.html'),
};
