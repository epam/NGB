import angular from 'angular';

import './ngbCoverageTable.scss';

import ngbCoverageTable from './ngbCoverageTable.component';
import controller from './ngbCoverageTable.controller';
import service from './ngbCoverageTable.service';
import ngbCoverageTableActions from './ngbCoverageTableActions';
import ngbCoverageTableFilters from './ngbCoverageTableFilters';

export default angular
    .module('ngbCoverageTable', [ngbCoverageTableActions, ngbCoverageTableFilters])
    .controller(controller.UID, controller)
    .component('ngbCoverageTable', ngbCoverageTable)
    .service('ngbCoverageTableService', service.instance)
    .name;
