import './ngbHeatmap.scss';

import angular from 'angular';

import component from './ngbHeatmap.component';
import controller from './ngbHeatmap.controller';

export default angular.module('ngbHeatmap', [])
    .controller(controller.UID, controller)
    .component('ngbHeatmap', component)
    .name;
