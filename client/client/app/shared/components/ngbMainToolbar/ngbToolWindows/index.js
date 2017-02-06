// Import Style
import './ngbToolWindows.scss';


import angular from 'angular';

// Import internal modules
import controller from './ngbToolWindows.controller';
import component from './ngbToolWindows.component';


export default angular.module('ngbToolWindows', [])
    .controller(controller.UID, controller)
    .component('ngbToolWindows',component)
    .name;
