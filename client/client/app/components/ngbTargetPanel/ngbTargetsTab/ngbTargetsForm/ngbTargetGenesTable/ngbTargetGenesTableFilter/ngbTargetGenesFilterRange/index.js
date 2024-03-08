import angular from 'angular';

import component from './ngbTargetGenesFilterRange.component';
import controller from './ngbTargetGenesFilterRange.controller';

export default angular.module('ngbTargetGenesFilterRange', [])
    .controller(controller.UID, controller)
    .component('ngbTargetGenesFilterRange', component)
    .name;