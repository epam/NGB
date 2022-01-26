import angular from 'angular';

import './ngbMotifsResultsFilterList.scss';

// Import internal modules
import component from './ngbMotifsResultsFilterList.component';
import controller from './ngbMotifsResultsFilterList.controller';

// Import external modules
export default angular.module('ngbMotifsResultsFilterList', [])
    .controller(controller.UID, controller)
    .component('ngbMotifsResultsFilterList', component)
    .name;
