import controller from './ngbGenesTable.controller';

export default {
    bindings: {
        isProgressShown: '='
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbGenesTable.tpl.html'),
};
