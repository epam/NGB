import angular from 'angular';

import './ngbTargetGenesTable.scss';

import component from './ngbTargetGenesTable.component';
import controller from './ngbTargetGenesTable.controller';
import service from './ngbTargetGenesTable.service';

import ngbGenesTablePagination from './ngbGenesTablePagination';
import ngbTargetGenesTableFilter from './ngbTargetGenesTableFilter';

export default angular
    .module('ngbTargetGenesTable', [ngbGenesTablePagination, ngbTargetGenesTableFilter])
    .controller(controller.UID, controller)
    .component('ngbTargetGenesTable', component)
    .service('ngbTargetGenesTableService', service.instance)
    .name;