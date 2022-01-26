import angular from 'angular';

import component from './ngbInternalPathwaysResult.component';

// Import internal modules
import controller from './ngbInternalPathwaysResult.controller';

// Import Style
import './ngbInternalPathwaysResult.scss';

import service from './ngbInternalPathwaysResult.service';

export default angular.module('ngbInternalPathwaysResult', [])
    .component('ngbInternalPathwaysResult', component)
    .controller(controller.UID, controller)
    .service('ngbInternalPathwaysResultService', service.instance)
    .name;
