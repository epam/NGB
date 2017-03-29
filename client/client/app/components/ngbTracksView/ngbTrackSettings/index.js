import './ngbTrackSettings.scss';

import angular from 'angular';

import controller from './ngbTrackSettings.controller';
import component from './ngbTrackSettings.component';
import trackCoverageSettings from './ngbTrackCoverageSettings';

export default angular.module('ngbTrackSettings', [trackCoverageSettings])
    .controller(controller.UID, controller)
    .component('ngbTrackSettings', component)
    .name;
