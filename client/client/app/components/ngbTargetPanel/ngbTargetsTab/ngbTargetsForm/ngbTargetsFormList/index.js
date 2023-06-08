import angular from 'angular';

import './ngbTargetsFormList.scss';

import component from './ngbTargetsFormList.component';
import controller from './ngbTargetsFormList.controller';

export default angular
    .module('ngbTargetsFormList', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsFormList', component)
    .name;
