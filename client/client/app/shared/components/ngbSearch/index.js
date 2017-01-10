// Import Style
import './ngbSearch.scss';


import angular from 'angular';

// Import internal modules
import constant from './ngbSearch.constant';
import controller from './ngbSearch.controller';
import component from './ngbSearch.components.js';

export default angular.module('ngbSearch', [])
    .constant('ngbSearchMessage', constant)
    .controller(controller.UID, controller)
    .component('ngbSearch', component)
    .name;