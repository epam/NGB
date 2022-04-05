import angular from 'angular';

import component from './ngbPathwayAnnotationMark.component';
import service from './ngbPathwayAnnotationMark.service';

// Import internal modules
import controller from './ngbPathwayAnnotationMark.controller';

// Import Style
import './ngbPathwayAnnotationMark.scss';

export default angular.module('ngbPathwayAnnotationMark', [])
    .service('ngbPathwayAnnotationMarkService', service.instance)
    .component('ngbPathwayAnnotationMark', component)
    .controller(controller.UID, controller)
    .name;
