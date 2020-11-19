import controller from './ngbWigResizePreference.controller';

export default {
    bindings: {
        applyToWigTracks: '=',
        height: '=',
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbWigResizePreference.tpl.html'),
};
