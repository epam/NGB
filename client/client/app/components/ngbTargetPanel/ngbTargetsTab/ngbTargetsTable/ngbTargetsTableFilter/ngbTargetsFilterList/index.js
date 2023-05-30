import angular from 'angular';

import component from './ngbTargetsFilterList.component';
import controller from './ngbTargetsFilterList.controller';

export default angular.module('ngbTargetsFilterList', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsFilterList', component)
    .name;
