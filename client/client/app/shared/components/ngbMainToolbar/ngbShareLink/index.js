import angular from 'angular';

// Import Style
import './ngbShareLink.scss';

// Import internal modules
import component from './ngbShareLink.component';
import controller from './ngbShareLink.controller';


export default angular.module('ngbShareLinkComponent', [])
    .component('ngbShareLink', component)
    .controller(controller.UID, controller)
    .name;