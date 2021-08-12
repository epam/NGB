import angular from 'angular';
import './ngbMotifsTablePagination.scss';

// Import internal modules
import component from './ngbMotifsTablePagination.component';
import controller from './ngbMotifsTablePagination.controller';


// Import external modules
export default angular.module('ngbMotifsTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbMotifsTablePagination', component)
    .name;
