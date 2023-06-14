import angular from 'angular';

import './ngbDiseasesTable.scss';

import component from './ngbDiseasesTable.component';
import controller from './ngbDiseasesTable.controller';
import service from './ngbDiseasesTable.service';

import ngbTablePagination from '../../ngbTablePagination';

export default angular
    .module('ngbDiseasesTable', [ngbTablePagination])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTable', component)
    .service('ngbDiseasesTableService', service.instance)
    .name;
