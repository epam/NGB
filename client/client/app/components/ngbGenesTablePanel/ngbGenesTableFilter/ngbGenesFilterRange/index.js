import './ngbGenesFilterRange.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbGenesFilterRange.component';
import controller from './ngbGenesFilterRange.controller';

// Import external modules
export default angular.module('ngbGenesFilterRange', [])
    .controller(controller.UID, controller)
    .component('ngbGenesFilterRange', component)
    .name;
