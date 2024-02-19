import angular from 'angular';

import './ngbPatentsSummary.scss';

import component from './ngbPatentsSummary.component';
import controller from './ngbPatentsSummary.controller';

export default angular
    .module('ngbPatentsSummary', [])
    .controller(controller.UID, controller)
    .component('ngbPatentsSummary', component)
    .name;