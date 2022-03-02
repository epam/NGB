import angular from 'angular';

import component from './ngbCoverageTableFilters.component';
import controller from './ngbCoverageTableFilters.controller';
import ngbCoverageFilterList from './ngbCoverageFilterList';
import ngbCoverageFilterRange from './ngbCoverageFilterRange';

export default angular
    .module('ngbCoverageTableFilters', [ngbCoverageFilterList, ngbCoverageFilterRange])
    .controller(controller.UID, controller)
    .component('ngbCoverageTableFilters', component)
    .name;
