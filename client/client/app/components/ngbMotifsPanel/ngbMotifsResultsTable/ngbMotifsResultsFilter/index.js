import angular from 'angular';

import './ngbMotifsResultsFilter.scss';

// Import internal modules
import component from './ngbMotifsResultsFilter.component';
import controller from './ngbMotifsResultsFilter.controller';
import ngbMotifsResultsFilterList from './ngbMotifsResultsFilterList';

// Import external modules
export default angular
    .module('ngbMotifsResultsFilter', [ngbMotifsResultsFilterList])
    .controller(controller.UID, controller)
    .component('ngbMotifsResultsFilter', component)
    .name;
