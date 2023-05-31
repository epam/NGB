import angular from 'angular';

import './ngbTargetsForm.scss';

import component from './ngbTargetsForm.component';
import controller from './ngbTargetsForm.controller';

export default angular
    .module('ngbTargetsForm', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsForm', component)
    .name;
