import controller from './ngbTrackActions.controller';

export default {
    restrict: 'EA',
    template: require('./ngbTrackActions.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings: {
        actions: '=',
        onHandle: '<',
        trackController: '<'
    }
};



