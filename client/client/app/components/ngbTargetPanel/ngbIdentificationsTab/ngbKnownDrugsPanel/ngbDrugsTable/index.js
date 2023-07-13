import angular from 'angular';

import './ngbDrugsTable.scss';

import component from './ngbDrugsTable.component';
import controller from './ngbDrugsTable.controller';
import service from './ngbDrugsTable.service';

import ngbDrugsTablePagination from './ngbDrugsTablePagination';
import ngbDrugsTableFilter from './ngbDrugsTableFilter';

export default angular
    .module('ngbDrugsTable', [ngbDrugsTablePagination, ngbDrugsTableFilter])
    .controller(controller.UID, controller)
    .component('ngbDrugsTable', component)
    .service('ngbDrugsTableService', service.instance)
    .name;
