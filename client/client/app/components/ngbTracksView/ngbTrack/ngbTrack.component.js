import controller from './ngbTrack.controller';

export default {
    bindings:{
        browserId: '=',
        menuManager: '=',
        selectable: '=',
        silentInteractions: '=',
        state: '=',
        track: '=',
        trackOpts: '=',
        viewport: '=',
        notification: '='
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbTrack.tpl.html')
};
