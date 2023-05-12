import angular from 'angular';

import './ngbTargetPanel.scss';

import component from './ngbTargetPanel.component';
import controller from './ngbTargetPanel.controller';
import service from './ngbTargetPanel.service';

import ngbTargetsTab from './ngbTargetsTab';

export default angular
    .module('ngbTargetPanel', [ngbTargetsTab])
    .controller(controller.UID, controller)
    .component('ngbTargetPanel', component)
    .service('ngbTargetPanelService', service.instance)
    .name;
