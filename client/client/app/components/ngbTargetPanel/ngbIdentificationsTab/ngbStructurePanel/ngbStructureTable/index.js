import angular from 'angular';

import './ngbStructureTable.scss';

import component from './ngbStructureTable.component';
import controller from './ngbStructureTable.controller';

import ngbStructureTableFilter from './ngbStructureTableFilter';
import ngbStructureTablePagination from './ngbStructureTablePagination';

export default angular
    .module('ngbStructureTable', [ngbStructureTableFilter, ngbStructureTablePagination])
    .controller(controller.UID, controller)
    .component('ngbStructureTable', component)
    .name;
