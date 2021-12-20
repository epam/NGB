import './ngbViewAction.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbViewAction.component';
import controller from './ngbViewAction.controller';

// Import external modules
export default angular.module('ngbViewAction', [])
    .controller(controller.UID, controller)
    .component('ngbViewAction', component)
    .name;
