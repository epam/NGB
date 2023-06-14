import angular from 'angular';

import './ngbTablePagination.scss';

import component from './ngbTablePagination.component';
import controller from './ngbTablePagination.controller';

export default angular
    .module('ngbTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbTablePagination', component)
    .name;
