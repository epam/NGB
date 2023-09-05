import angular from 'angular';

import './ngbGenomicsTablePagination.scss';

import component from './ngbGenomicsTablePagination.component';
import controller from './ngbGenomicsTablePagination.controller';

export default angular
    .module('ngbGenomicsTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbGenomicsTablePagination', component)
    .name;
