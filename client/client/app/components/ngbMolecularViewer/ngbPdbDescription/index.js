import angular from 'angular';

// Import Style
import './ngbPdbDescription.scss';

// Import internal modules
import component from './ngbPdbDescription.component';
import controller from './ngbPdbDescription.controller';

export default angular.module('ngbPdbDescriptionComponent', [])
    .component('ngbPdbDescription', component)
    .controller(controller.UID, controller)
    .name;
