import angular from 'angular';

import './ngbPatentsPanel.scss';

import component from './ngbPatentsPanel.component';
import controller from './ngbPatentsPanel.controller';
import service from './ngbPatentsPanel.service';

import ngbPatentsSequencesTab from './ngbPatentsSequencesTab';
import ngbPatentsChemicalsTab from './ngbPatentsChemicalsTab';
import ngbPatentsGeneralTab from './ngbPatentsGeneralTab';

export default angular
    .module('ngbPatentsPanel', [ngbPatentsSequencesTab, ngbPatentsChemicalsTab, ngbPatentsGeneralTab])
    .controller(controller.UID, controller)
    .component('ngbPatentsPanel', component)
    .service('ngbPatentsPanelService', service.instance)
    .name;