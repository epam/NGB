import angular from 'angular';

import './ngbDiseasesTablePagination.scss';

import component from './ngbDiseasesTablePagination.component';
import controller from './ngbDiseasesTablePagination.controller';

export default angular
    .module('ngbDiseasesTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTablePagination', component)
    .name;
