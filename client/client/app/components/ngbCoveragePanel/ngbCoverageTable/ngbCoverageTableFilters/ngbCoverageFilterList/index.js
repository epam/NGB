import angular from 'angular';

import './ngbCoverageFilterList.scss';

import component from './ngbCoverageFilterList.component';
import controller from './ngbCoverageFilterList.controller';

export default angular.module('ngbCoverageFilterList', [])
    .controller(controller.UID, controller)
    .component('ngbCoverageFilterList', component)
    .name;
