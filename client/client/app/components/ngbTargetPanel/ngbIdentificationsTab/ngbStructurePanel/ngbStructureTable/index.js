import angular from 'angular';

import './ngbStructureTable.scss';

import component from './ngbStructureTable.component';
import controller from './ngbStructureTable.controller';

import ngbStructureTablePagination from './ngbStructureTablePagination';

export default angular
    .module('ngbStructureTable', [ngbStructureTablePagination])
    .controller(controller.UID, controller)
    .component('ngbStructureTable', component)
    .name;
