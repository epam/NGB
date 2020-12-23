import controller from './ngbTracksSelectionMenu.controller';

export default {
    restrict: 'EA',
    template: require('./ngbTracksSelectionMenu.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings: {
        browserId: '='
    }
};



