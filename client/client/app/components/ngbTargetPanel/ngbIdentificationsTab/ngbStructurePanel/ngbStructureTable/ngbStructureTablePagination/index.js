import angular from 'angular';

import './ngbStructureTablePagination.scss';

import component from './ngbStructureTablePagination.component';
import controller from './ngbStructureTablePagination.controller';

export default angular
    .module('ngbStructureTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbStructureTablePagination', component)
    .name;
