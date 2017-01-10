// Import Style
import './ngbPanelErrorList.scss';


import angular from 'angular';

// Import internal modules
import controller from './ngbPanelErrorList.controller';
import component from './ngbPanelErrorList.component';


export default angular.module('ngbPanelErrorList', [])
    .controller(controller.UID, controller)
    .component('ngbPanelErrorList', component)
    .name;
