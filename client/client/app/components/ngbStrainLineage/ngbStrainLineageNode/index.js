import angular from 'angular';

import component from './ngbStrainLineageNode.component';

// Import internal modules
import controller from './ngbStrainLineageNode.controller';

// Import Style
import './ngbStrainLineageNode.scss';

export default angular.module('ngbStrainLineageNode', [])
    .component('ngbStrainLineageNode', component)
    .controller(controller.UID, controller)
    .name;
