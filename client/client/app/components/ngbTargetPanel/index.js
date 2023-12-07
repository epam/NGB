import angular from 'angular';

import './ngbTargetPanel.scss';

import component from './ngbTargetPanel.component';
import controller from './ngbTargetPanel.controller';
import service from './ngbTargetPanel.service';
import llmService from './ngbTargetLLM.service';

import ngbTargetsTab from './ngbTargetsTab';
import ngbIdentificationsTab from './ngbIdentificationsTab';
import ngbDiseasesTab from './ngbDiseasesTab';

export default angular
    .module('ngbTargetPanel', [ngbTargetsTab, ngbIdentificationsTab, ngbDiseasesTab])
    .controller(controller.UID, controller)
    .component('ngbTargetPanel', component)
    .service('ngbTargetPanelService', service.instance)
    .service(llmService.UID, llmService.instance)
    .name;
