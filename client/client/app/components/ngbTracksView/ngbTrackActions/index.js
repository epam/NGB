import './ngbTrackActions.scss';

import angular from 'angular';

import controller from './ngbTrackActions.controller';
import component from './ngbTrackActions.component';

export default angular.module('ngbTrackActions', [])
    .controller(controller.UID, controller)
    .component('ngbTrackActions', component)
    .name;
