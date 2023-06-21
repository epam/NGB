import angular from 'angular';

import './ngbDiseasesTable.scss';

import component from './ngbDiseasesTable.component';
import controller from './ngbDiseasesTable.controller';
import service from './ngbDiseasesTable.service';

import ngbTablePagination from '../../ngbTablePagination';
import ngbDiseasesTableFilter from './ngbDiseasesTableFilter';

export default angular
    .module('ngbDiseasesTable', [ngbTablePagination, ngbDiseasesTableFilter])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTable', component)
    .service('ngbDiseasesTableService', service.instance)
    .name;
