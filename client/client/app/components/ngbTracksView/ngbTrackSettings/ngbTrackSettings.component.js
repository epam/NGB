import controller from './ngbTrackSettings.controller';

export default {
    restrict: 'EA',
    template: require('./ngbTrackSettings.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings: {
        settings: '='
    }
};



