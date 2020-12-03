import controller from './ngbTrack.controller';

export default {
    bindings:{
        menuManager: '=',
        selectable: '=',
        silentInteractions: '=',
        state: '=',
        track: '=',
        trackOpts: '=',
        viewport: '='
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbTrack.tpl.html')
};
