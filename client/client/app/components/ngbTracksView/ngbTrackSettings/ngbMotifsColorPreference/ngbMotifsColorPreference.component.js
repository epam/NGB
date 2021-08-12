import controller from './ngbMotifsColorPreference.controller';

export default {
    bindings: {
        applyToAllTracks: '=',
        applyToCurrentTrack: '=',
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbMotifsColorPreference.dialog.tpl.html'),
};

