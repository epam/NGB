import './ngbHeatmapPanel.scss';

import angular from 'angular';

import component from './ngbHeatmapPanel.component';
import controller from './ngbHeatmapPanel.controller';

export default angular.module('ngbHeatmapPanel', [])
    .controller(controller.UID, controller)
    .component('ngbHeatmapPanel', component)
    .name;
