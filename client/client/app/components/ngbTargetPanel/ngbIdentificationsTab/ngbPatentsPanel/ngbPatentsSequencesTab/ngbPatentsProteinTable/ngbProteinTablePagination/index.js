import angular from 'angular';

import './ngbProteinTablePagination.scss';

import component from './ngbProteinTablePagination.component';
import controller from './ngbProteinTablePagination.controller';

export default angular
    .module('ngbProteinTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbProteinTablePagination', component)
    .name;
