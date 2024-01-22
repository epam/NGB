import angular from 'angular';

import './ngbTargetGenesList.scss';

import component from './ngbTargetGenesList.component';
import controller from './ngbTargetGenesList.controller';

export default angular
    .module('ngbTargetGenesList', [])
    .controller(controller.UID, controller)
    .component('ngbTargetGenesList', component)
    .name;
