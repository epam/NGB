import angular from 'angular';

import './ngbPagination.scss';

import component from './ngbPagination.component';
import controller from './ngbPagination.controller';

export default angular
    .module('ngbPagination', [])
    .controller(controller.UID, controller)
    .component('ngbPagination', component)
    .name;
