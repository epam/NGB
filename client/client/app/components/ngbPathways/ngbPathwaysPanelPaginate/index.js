import angular from 'angular';

// Import internal modules
import component from './ngbPathwaysPanelPaginate.component';
import controller from './ngbPathwaysPanelPaginate.controller';
import './ngbPathwaysPanelPaginate.scss';


// Import external modules
export default angular.module('ngbPathwaysPanelPaginate', [])
    .controller(controller.UID, controller)
    .component('ngbPathwaysPanelPaginate', component)
    .name;
