import angular from 'angular';
import './ngbHomologsPanelPaginate.scss';

// Import internal modules
import component from './ngbHomologsPanelPaginate.component';
import controller from './ngbHomologsPanelPaginate.controller';


// Import external modules
export default angular.module('ngbHomologsPanelPaginate', [])
    .controller(controller.UID, controller)
    .component('ngbHomologsPanelPaginate', component)
    .name;
