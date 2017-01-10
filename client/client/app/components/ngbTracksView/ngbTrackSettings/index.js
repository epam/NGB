import './ngbTrackSettings.scss';

import angular from 'angular';

import controller from './ngbTrackSettings.controller';
import component from './ngbTrackSettings.component';

export default angular.module('ngbTrackSettings', [])
    .controller(controller.UID, controller)
    .component('ngbTrackSettings', component)
    .name;
