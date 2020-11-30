import controller from './ngbWigOpacityPreference.controller';

export default {
    bindings: {
        applyToCurrentTrack: '=',
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbWigOpacityPreference.dialog.tpl.html'),
};

