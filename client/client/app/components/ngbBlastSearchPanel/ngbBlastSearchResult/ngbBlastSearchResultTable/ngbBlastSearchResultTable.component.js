import controller from './ngbBlastSearchResultTable.controller';

export default {
    bindings: {
        changeState: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbBlastSearchResultTable.tpl.html'),
};
