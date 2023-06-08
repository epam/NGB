import angular from 'angular';

import './ngbTargetsForm.scss';

import component from './ngbTargetsForm.component';
import controller from './ngbTargetsForm.controller';

import ngbTargetsFormList from './ngbTargetsFormList';

export default angular
    .module('ngbTargetsForm', [ngbTargetsFormList])
    .controller(controller.UID, controller)
    .component('ngbTargetsForm', component)
    .name;
