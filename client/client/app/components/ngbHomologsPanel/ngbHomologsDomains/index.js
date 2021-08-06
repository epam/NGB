// Import Style
import './ngbHomologsDomains.scss';

import angular from 'angular';
import component from './ngbHomologsDomains.component';
import controller from './ngbHomologsDomains.controller';

export default angular
    .module('ngbHomologsDomains', [])
    .controller(controller.UID, controller)
    .component('ngbHomologsDomains', component)
    .name;
