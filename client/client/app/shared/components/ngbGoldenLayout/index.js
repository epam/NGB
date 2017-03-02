// Import Style
import './ngbGoldenLayout.scss';

import angular from 'angular';


// Import internal modules
import component from './ngbGoldenLayout.component';
import constant from './ngbGoldenLayout.constant';
import controller from './ngbGoldenLayout.controller';
import directive from './ngbGoldenLayout.directive';
import goldenLayout from './ngbGoldenLayoutObj.service';
import service from './ngbGoldenLayout.service';

// Import dependencies
import ngbViewActionsConstant from './ngbViewActions/ngbViewActions.constant';
import ngbCoordinates from './ngbCoordinates';
import ngbViewActions from './ngbViewActions';

export default angular.module('ngbGoldenLayout', [ngbCoordinates, ...ngbViewActions])
    .controller(controller.UID, controller)
    .constant('ngbViewActionsConstant', ngbViewActionsConstant)
    .constant('ngbGoldenLayoutConstant', constant)
    .service('GoldenLayout', goldenLayout.instance)
    .service('ngbGoldenLayoutService', service.instance)
    .component('ngbGoldenLayout', component)
    .directive('ngbGoldenLayoutContainer', directive)
    .name;
