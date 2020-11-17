import controller from './ngbWigColorPreference.controller';

export default {
    bindings: {
        applyToCurrentTrack: '=',
        applyToWigTracks: '=',
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbWigColorPreference.dialog.tpl.html'),
};

