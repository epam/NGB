import angular from 'angular';

import './ngbGenesTablePagination.scss';

import component from './ngbGenesTablePagination.component';
import controller from './ngbGenesTablePagination.controller';

export default angular
    .module('ngbGenesTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbGenesTablePagination', component)
    .name;
