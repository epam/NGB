import angular from 'angular';

import component from './ngbTargetsFilterInput.component';
import controller from './ngbTargetsFilterInput.controller';

export default angular.module('ngbTargetsFilterInput', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsFilterInput', component)
    .name;
