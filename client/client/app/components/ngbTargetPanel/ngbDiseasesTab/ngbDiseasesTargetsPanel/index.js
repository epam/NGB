import angular from 'angular';

import './ngbDiseasesTargetsPanel.scss';

import component from './ngbDiseasesTargetsPanel.component';
import controller from './ngbDiseasesTargetsPanel.controller';
import service from './ngbDiseasesTargetsPanel.service';

import ngbTargetsTablePagination from './ngbTargetsTablePagination';
import ngbDiseasesTargetsTableFilter from './ngbDiseasesTargetsTableFilter';
import ngbTargetsTableContextMenu from './ngbTargetsTableContextMenu';

export default angular
    .module('ngbDiseasesTargetsPanel', [
        ngbTargetsTablePagination,
        ngbDiseasesTargetsTableFilter,
        ngbTargetsTableContextMenu
    ])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTargetsPanel', component)
    .service('ngbDiseasesTargetsPanelService', service.instance)
    .name;