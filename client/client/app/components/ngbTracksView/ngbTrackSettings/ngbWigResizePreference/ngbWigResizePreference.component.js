import controller from './ngbWigResizePreference.controller';

export default {
    bindings: {
        applyToWigTracks: '=',
        height: '=',
        maxHeight: '<',
        minHeight: '<',
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbWigResizePreference.tpl.html'),
};
