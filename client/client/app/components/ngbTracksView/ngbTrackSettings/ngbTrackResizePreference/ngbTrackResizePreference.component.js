import controller from './ngbTrackResizePreference.controller';

export default {
    bindings: {
        applyToAllTracks: '=',
        applyToAllTracksOfType: '=',
        applyToAllTracksOfTypeVisible: '<',
        applyToAllTracksVisible: '<',
        height: '=',
        maxHeight: '<',
        minHeight: '<',
        types: '<'
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbTrackResizePreference.tpl.html'),
};
