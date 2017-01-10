// Import Style
import './ngbTrack.scss';
import angular from 'angular';
import controller from './ngbTrack.controller';
import component from './ngbTrack.component';
import dataServices from '../../../../dataServices/angular-module';
// Import internal modules

export default angular.module('ngbRulerTrack', [dataServices])
    .controller(controller.UID, controller)
    .component('ngbRulerTrack', component)
    .name;
    