import './ngbTrackFontSize.scss';

import angular from 'angular';

import component from './ngbTrackFontSize.component';
import controller from './ngbTrackFontSize.controller';
import run from './ngbTrackFontSize.run';

export default angular.module('ngbTrackFontSize', [])
    .controller(controller.UID, controller)
    .component('ngbTrackFontSize', component)
    .run(run)
    .name;
