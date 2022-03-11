import angular from 'angular';

import component from './ngbInternalPathwayNode.component';

// Import internal modules
import controller from './ngbInternalPathwayNode.controller';

// Import Style
import './ngbInternalPathwayNode.scss';

export default angular.module('ngbInternalPathwayNode', [])
    .component('ngbInternalPathwayNode', component)
    .controller(controller.UID, controller)
    .name;
