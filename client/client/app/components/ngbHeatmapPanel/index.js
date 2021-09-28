import './ngbHeatmapPanel.scss';

import angular from 'angular';

import component from './ngbHeatmapPanel.component';
import controller from './ngbHeatmapPanel.controller';
import service from './ngbHeatmapPanel.service';

export default angular.module('ngbHeatmapPanel', [])
    .service(service.UID, service.instance)
    .controller(controller.UID, controller)
    .component('ngbHeatmapPanel', component)
    .name;
