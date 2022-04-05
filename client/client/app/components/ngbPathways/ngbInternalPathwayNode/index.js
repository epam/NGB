import angular from 'angular';

import component from './ngbInternalPathwayNode.component';
import service from './ngbInternalPathwayNode.service';

// Import internal modules
import controller from './ngbInternalPathwayNode.controller';
import annotationMark from './ngbPathwayAnnotationMark';

// Import Style
import './ngbInternalPathwayNode.scss';

export default angular.module('ngbInternalPathwayNode', [annotationMark])
    .service('ngbInternalPathwayNodeService', service.instance)
    .component('ngbInternalPathwayNode', component)
    .controller(controller.UID, controller)
    .name;
