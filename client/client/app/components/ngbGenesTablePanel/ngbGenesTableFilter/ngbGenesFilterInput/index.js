import './ngbGenesFilterInput.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbGenesFilterInput.component';
import controller from './ngbGenesFilterInput.controller';

// Import external modules
export default angular.module('ngbGenesFilterInput', [])
    .controller(controller.UID, controller)
    .component('ngbGenesFilterInput', component)
    .name;
