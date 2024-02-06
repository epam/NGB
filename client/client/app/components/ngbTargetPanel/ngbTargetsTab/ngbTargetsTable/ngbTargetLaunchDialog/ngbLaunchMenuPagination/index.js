import angular from 'angular';

import './ngbLaunchMenuPagination.scss';

import component from './ngbLaunchMenuPagination.component';
import controller from './ngbLaunchMenuPagination.controller';

export default angular
    .module('ngbLaunchMenuPagination', [])
    .controller(controller.UID, controller)
    .component('ngbLaunchMenuPagination', component)
    .name;
