// Import Style
import './ngbDataSets.scss';
// Import internal modules
import angular from 'angular';
import component from './ngbDataSets.component';
import controller from './ngbDataSets.controller';
import service from './ngbDataSets.service';
import indeterminateCheckbox from './internal/ngbDataSets.indeterminateCheckbox';
import ngbContextMenu from './internal/ngbDataSetContextMenu/ngbContextMenu';
import ngbDataSetContextMenu from './internal/ngbDataSetContextMenu';

export default angular.module('ngbDataSets', [ngbContextMenu, ngbDataSetContextMenu])
    .directive('indeterminateCheckbox', indeterminateCheckbox)
    .service('ngbDataSetsService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbDataSets', component)
    .name;
