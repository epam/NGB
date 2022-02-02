import controller from './ngbInternalPathwaysTable.controller';

export default {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbInternalPathwaysTable.tpl.html'),
};
