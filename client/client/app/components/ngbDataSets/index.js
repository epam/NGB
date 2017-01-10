// Import Style
import './ngbDataSets.scss';
// Import internal modules
import angular from 'angular';
import component from './ngbDataSets.component';
import controller from './ngbDataSets.controller';
import ivhTreeview from '../../compat/angular-ivh-treeview';
import service from './ngbDataSets.service';

export default angular.module('ngbDataSets', [ivhTreeview])
    .service('ngbDataSetsService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbDataSets', component)
    .name;
