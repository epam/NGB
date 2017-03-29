import controller from './ngbTrackCoverageSettings.controller';

export default {
    restrict: 'EA',
    template: require('./ngbTrackCoverageSettings.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings: {
        from: '=',
        to: '=',
        applyToCurrentTrack: '=',
        applyToWigTracks: '=',
        applyToBamTracks: '=',
        isLogScale: '='
    }
};



