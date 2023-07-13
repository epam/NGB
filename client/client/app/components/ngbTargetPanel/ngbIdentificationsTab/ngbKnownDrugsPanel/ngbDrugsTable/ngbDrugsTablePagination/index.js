import angular from 'angular';

import './ngbDrugsTablePagination.scss';

import component from './ngbDrugsTablePagination.component';
import controller from './ngbDrugsTablePagination.controller';

export default angular
    .module('ngbDrugsTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbDrugsTablePagination', component)
    .name;
