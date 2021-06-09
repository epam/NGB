import './ngbBlastSearchAlignmentList.scss';
import  './ngbBlastSearchAlignment/ngbBlastSearchAlignment.scss';
import angular from 'angular';

// Import internal modules
import component from './ngbBlastSearchAlignmentList.component';
import controller from './ngbBlastSearchAlignmentList.controller';
import itemComponent from './ngbBlastSearchAlignment/ngbBlastSearchAlignment.component';
import itemController from './ngbBlastSearchAlignment/ngbBlastSearchAlignment.controller';
import itemService from './ngbBlastSearchAlignment/ngbBlastSearchAlignment.service';

export default angular.module('ngbBlastSearchAlignment', [])
    .service('ngbBlastSearchAlignmentService', itemService)
    .controller(itemController.UID, itemController)
    .component('ngbBlastSearchAlignment', itemComponent)
    .controller(controller.UID, controller)
    .component('ngbBlastSearchAlignmentList', component)
    .name;
