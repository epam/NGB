import angular from 'angular';

import './ngbTargetsTableMenu.scss';

import component from './ngbTargetsTableMenu.component';
import controller from './ngbTargetsTableMenu.controller';

export default angular
    .module('ngbTargetsTableMenu', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsTableMenu', component)
    .name;
