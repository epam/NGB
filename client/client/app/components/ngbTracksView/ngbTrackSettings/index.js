import './ngbTrackSettings.scss';

import angular from 'angular';

import controller from './ngbTrackSettings.controller';
import component from './ngbTrackSettings.component';
import trackCoverageSettings from './ngbTrackCoverageSettings';
import wigColorPreference from './ngbWigColorPreference';

export default angular.module('ngbTrackSettings', [trackCoverageSettings, wigColorPreference])
    .controller(controller.UID, controller)
    .component('ngbTrackSettings', component)
    .name;
