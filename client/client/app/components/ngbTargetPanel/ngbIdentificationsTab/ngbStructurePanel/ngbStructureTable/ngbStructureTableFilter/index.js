import angular from 'angular';

import './ngbStructureTableFilter.scss';

import component from './ngbStructureTableFilter.component';
import controller from './ngbStructureTableFilter.controller';

export default angular.module('ngbStructureTableFilter', [])
    .controller(controller.UID, controller)
    .component('ngbStructureTableFilter', component)
    .name;
