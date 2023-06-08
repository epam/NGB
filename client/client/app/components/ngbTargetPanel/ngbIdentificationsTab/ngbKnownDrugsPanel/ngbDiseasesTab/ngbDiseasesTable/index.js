import angular from 'angular';

import './ngbDiseasesTable.scss';

import component from './ngbDiseasesTable.component';
import controller from './ngbDiseasesTable.controller';

import ngbTablePagination from '../../ngbTablePagination';

export default angular
    .module('ngbDiseasesTable', [ngbTablePagination])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTable', component)
    .name;
