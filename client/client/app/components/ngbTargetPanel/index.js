import angular from 'angular';

import './ngbTargetPanel.scss';

import component from './ngbTargetPanel.component';
import controller from './ngbTargetPanel.controller';
import service from './ngbTargetPanel.service';

import ngbTargetsTab from './ngbTargetsTab';
import ngbIdentificationsTab from './ngbIdentificationsTab';

export default angular
    .module('ngbTargetPanel', [ngbTargetsTab, ngbIdentificationsTab])
    .controller(controller.UID, controller)
    .component('ngbTargetPanel', component)
    .service('ngbTargetPanelService', service.instance)
    .name;
