import angular from 'angular';

import './ngbTargetsTablePaginate.scss';

import component from './ngbTargetsTablePaginate.component';
import controller from './ngbTargetsTablePaginate.controller';

export default angular.module('ngbTargetsTablePaginate', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsTablePaginate', component)
    .name;
