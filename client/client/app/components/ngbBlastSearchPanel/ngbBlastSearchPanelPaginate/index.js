import angular from 'angular';
import './ngbBlastSearchPanelPaginate.scss';

// Import internal modules
import component from './ngbBlastSearchPanelPaginate.component';
import controller from './ngbBlastSearchPanelPaginate.controller';


// Import external modules
export default angular.module('ngbBlastSearchPanelPaginate', [])
    .controller(controller.UID, controller)
    .component('ngbBlastSearchPanelPaginate', component)
    .name;