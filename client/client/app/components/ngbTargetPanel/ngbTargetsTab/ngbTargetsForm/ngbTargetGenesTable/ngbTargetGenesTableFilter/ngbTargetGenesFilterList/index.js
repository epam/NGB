import angular from 'angular';

import './ngbTargetGenesFilterList.scss';

import component from './ngbTargetGenesFilterList.component';
import controller from './ngbTargetGenesFilterList.controller';

export default angular.module('ngbTargetGenesFilterList', [])
    .controller(controller.UID, controller)
    .component('ngbTargetGenesFilterList', component)
    .name;