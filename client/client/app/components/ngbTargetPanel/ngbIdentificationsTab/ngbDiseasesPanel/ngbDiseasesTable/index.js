import angular from 'angular';

import './ngbDiseasesTable.scss';

import component from './ngbDiseasesTable.component';
import controller from './ngbDiseasesTable.controller';
import service from './ngbDiseasesTable.service';

import ngbDiseasesTablePagination from './ngbDiseasesTablePagination';
import ngbDiseasesTableFilter from './ngbDiseasesTableFilter';
import ngbDiseasesTableContextMenu from './ngbDiseasesTableContextMenu';

export default angular
    .module('ngbDiseasesTable', [
        ngbDiseasesTablePagination,
        ngbDiseasesTableFilter,
        ngbDiseasesTableContextMenu
    ])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTable', component)
    .service('ngbDiseasesTableService', service.instance)
    .name;
