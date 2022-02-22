import angular from 'angular';
import fileModel from './fileModel.directive';

import component from './ngbPathwaysAnnotation.component';
import controller from './ngbPathwaysAnnotation.controller';
import './ngbPathwaysAnnotation.scss';
import service from './ngbPathwaysAnnotation.service';

export default angular
    .module('ngbPathwaysAnnotation', [])
    .service('ngbPathwaysAnnotationService', service)
    .directive('fileModel', fileModel)
    .controller(controller.UID, controller)
    .component('ngbPathwaysAnnotation', component)
    .name;
