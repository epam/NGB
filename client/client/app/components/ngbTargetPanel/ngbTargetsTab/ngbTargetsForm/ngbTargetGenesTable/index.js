import angular from 'angular';

import './ngbTargetGenesTable.scss';

import component from './ngbTargetGenesTable.component';
import controller from './ngbTargetGenesTable.controller';
import service from './ngbTargetGenesTable.service';

import ngbGenesTablePagination from './ngbGenesTablePagination';
import ngbGenesTableFilter from './ngbGenesTableFilter';

export default angular
    .module('ngbTargetGenesTable', [ngbGenesTablePagination, ngbGenesTableFilter])
    .controller(controller.UID, controller)
    .component('ngbTargetGenesTable', component)
    .service('ngbTargetGenesTableService', service.instance)
    .name;