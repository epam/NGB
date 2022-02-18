import angular from 'angular';

import './ngbCoverageTable.scss';

import ngbCoverageTable from './ngbCoverageTable.component';
import controller from './ngbCoverageTable.controller';

export default angular.module('ngbCoverageTable', [])
    .controller(controller.UID, controller)
    .component('ngbCoverageTable', ngbCoverageTable)
    .name;
