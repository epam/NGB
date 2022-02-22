import angular from 'angular';

import './ngbCoverageTableActions.scss';

import component from './ngbCoverageTableActions.component';
import controller from './ngbCoverageTableActions.controller';

export default angular.module('ngbCoverageTableActions', [])
    .controller(controller.UID, controller)
    .component('ngbCoverageTableActions', component)
    .name;
