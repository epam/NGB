import angular from 'angular';

import './ngbPatentsProteinTable.scss';

import component from './ngbPatentsProteinTable.component';
import controller from './ngbPatentsProteinTable.controller';

import ngbProteinTablePagination from './ngbProteinTablePagination';

export default angular
    .module('ngbPatentsProteinTable', [ngbProteinTablePagination])
    .controller(controller.UID, controller)
    .component('ngbPatentsProteinTable', component)
    .name;