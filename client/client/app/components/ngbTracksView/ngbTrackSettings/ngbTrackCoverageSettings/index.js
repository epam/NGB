import './ngbTrackCoverageSettings.scss';

import angular from 'angular';

import controller from './ngbTrackCoverageSettings.controller';
import component from './ngbTrackCoverageSettings.component';
import run from './ngbTrackCoverageSettings.run';

export default angular.module('ngbTrackCoverageSettings', [])
    .controller(controller.UID, controller)
    .component('ngbTrackCoverageSettings', component)
    .run(run)
    .name;
