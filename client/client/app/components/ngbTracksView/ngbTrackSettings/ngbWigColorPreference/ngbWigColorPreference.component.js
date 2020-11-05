import controller from './ngbWigColorPreference.controller';

export default {
    restrict: 'EA',
    template: require('./ngbWigColorPreference.dialog.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings: {
        applyToCurrentTrack: '=',
    }
};

