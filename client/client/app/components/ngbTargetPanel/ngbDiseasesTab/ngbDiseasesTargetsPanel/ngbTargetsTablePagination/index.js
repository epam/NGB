import angular from 'angular';

import './ngbTargetsTablePagination.scss';

import component from './ngbTargetsTablePagination.component';
import controller from './ngbTargetsTablePagination.controller';

export default angular
    .module('ngbTargetsTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsTablePagination', component)
    .name;
