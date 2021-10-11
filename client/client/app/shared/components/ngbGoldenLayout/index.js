// Import Style
import angular from 'angular';
import ngbCoordinates from './ngbCoordinates';

// Import internal modules
import component from './ngbGoldenLayout.component';
import constant from './ngbGoldenLayout.constant';
import controller from './ngbGoldenLayout.controller';
import directive from './ngbGoldenLayout.directive';
import './ngbGoldenLayout.scss';
import service from './ngbGoldenLayout.service';
import goldenLayout from './ngbGoldenLayoutObj.service';
import ngbViewActions from './ngbViewActions';

// Import dependencies
import ngbViewActionsConstant from './ngbViewActions/ngbViewActions.constant';
import './ngbViewActions/ngbViewActions.scss';

export default angular.module('ngbGoldenLayout', [ngbCoordinates, ...ngbViewActions])
    .controller(controller.UID, controller)
    .constant('ngbViewActionsConstant', ngbViewActionsConstant)
    .constant('ngbGoldenLayoutConstant', constant)
    .service('GoldenLayout', goldenLayout.instance)
    .service('ngbGoldenLayoutService', service.instance)
    .component('ngbGoldenLayout', component)
    .directive('ngbGoldenLayoutContainer', directive)
    .name;
