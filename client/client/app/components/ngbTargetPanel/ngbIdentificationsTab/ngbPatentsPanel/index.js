import angular from 'angular';

import './ngbPatentsPanel.scss';

import component from './ngbPatentsPanel.component';
import controller from './ngbPatentsPanel.controller';

import ngbPatentsSequencesTab from './ngbPatentsSequencesTab';

export default angular
    .module('ngbPatentsPanel', [ngbPatentsSequencesTab])
    .controller(controller.UID, controller)
    .component('ngbPatentsPanel', component)
    .name;