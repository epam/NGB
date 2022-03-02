import angular from 'angular';

import './ngbCoverageFilterRange.scss';

import component from './ngbCoverageFilterRange.component';
import controller from './ngbCoverageFilterRange.controller';

export default angular.module('ngbCoverageFilterRange', [])
    .controller(controller.UID, controller)
    .component('ngbCoverageFilterRange', component)
    .name;
