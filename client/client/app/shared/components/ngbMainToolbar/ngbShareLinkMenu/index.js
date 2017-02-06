import angular from 'angular';

// Import Style
import './ngbShareLinkMenu.scss';

// Import internal modules
import component from './ngbShareLinkMenu.component';
import controller from './ngbShareLinkMenu.controller';

export default angular.module('ngbShareLinkMenuComponent', [])
    .component('ngbShareLinkMenu', component)
    .controller(controller.UID, controller)
    .name;
