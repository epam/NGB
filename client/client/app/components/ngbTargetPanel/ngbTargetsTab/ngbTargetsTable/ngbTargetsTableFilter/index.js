import './ngbTargetsTableFilter.scss';

import angular from 'angular';

import component from './ngbTargetsTableFilter.component';
import controller from './ngbTargetsTableFilter.controller';

export default angular.module('ngbTargetsTableFilter', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsTableFilter', component)
    .name;
