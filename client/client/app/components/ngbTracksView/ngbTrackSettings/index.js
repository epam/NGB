import './ngbTrackSettings.scss';

import angular from 'angular';

import component from './ngbTrackSettings.component';
import controller from './ngbTrackSettings.controller';
import trackCoverageSettings from './ngbTrackCoverageSettings';
import wigColorPreference from './ngbWigColorPreference';
import wigOpacityPreference from './ngbWigOpacityPreference';
import wigResizePreference from './ngbWigResizePreference';

export default angular
    .module('ngbTrackSettings', [
        trackCoverageSettings,
        wigResizePreference,
        wigColorPreference,
        wigOpacityPreference,
    ])
    .controller(controller.UID, controller)
    .component('ngbTrackSettings', component).name;
