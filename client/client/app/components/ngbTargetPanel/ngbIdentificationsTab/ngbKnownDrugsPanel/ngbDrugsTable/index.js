import angular from 'angular';

import './ngbDrugsTable.scss';

import component from './ngbDrugsTable.component';
import controller from './ngbDrugsTable.controller';

import ngbTablePagination from '../ngbTablePagination';

export default angular
    .module('ngbDrugsTable', [ngbTablePagination])
    .controller(controller.UID, controller)
    .component('ngbDrugsTable', component)
    .name;
