import controller from './ngbTrack.controller';

export default {
    bindings:{
        menuManager: '=',
        track: '=',
        trackOpts: '=',
        viewport: '=',
        silentInteractions: '=',
        state: '='
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbTrack.tpl.html')
};
