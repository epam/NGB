import controller from './ngbBedColorPreference.controller';

export default {
    bindings: {
        applyToAllTracks: '=',
        applyToCurrentTrack: '=',
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbBedColorPreference.dialog.tpl.html'),
};

