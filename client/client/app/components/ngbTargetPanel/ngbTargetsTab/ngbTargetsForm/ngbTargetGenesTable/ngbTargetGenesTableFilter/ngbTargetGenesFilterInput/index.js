import angular from 'angular';

import component from './ngbTargetGenesFilterInput.component';
import controller from './ngbTargetGenesFilterInput.controller';

export default angular.module('ngbTargetGenesFilterInput', [])
    .controller(controller.UID, controller)
    .component('ngbTargetGenesFilterInput', component)
    .name;