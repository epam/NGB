import angular from 'angular';

import './ngbPatentsPanel.scss';

import component from './ngbPatentsPanel.component';
import controller from './ngbPatentsPanel.controller';

import ngbPatentsSequencesTab from './ngbPatentsSequencesTab';
import ngbPatentsChemicalsTab from './ngbPatentsChemicalsTab';

export default angular
    .module('ngbPatentsPanel', [ngbPatentsSequencesTab, ngbPatentsChemicalsTab])
    .controller(controller.UID, controller)
    .component('ngbPatentsPanel', component)
    .name;