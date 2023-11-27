import angular from 'angular';

import './ngbPatentsPanel.scss';

import component from './ngbPatentsPanel.component';
import controller from './ngbPatentsPanel.controller';
import service from './ngbPatentsPanel.service';

export default angular
    .module('ngbPatentsPanel', [])
    .controller(controller.UID, controller)
    .component('ngbPatentsPanel', component)
    .service('ngbPatentsPanelService', service.instance)
    .name;