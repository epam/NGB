import controller from './ngbTrackResizePreference.controller';

export default {
    bindings: {
        applyToAllTracksTitle: '=',
        applyToAllTracks: '=',
        height: '=',
        maxHeight: '<',
        minHeight: '<',
        multiple: '<'
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbTrackResizePreference.tpl.html'),
};
