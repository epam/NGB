import angular from 'angular';

import './ngbCoverageTable.scss';

import ngbCoverageTable from './ngbCoverageTable.component';
import controller from './ngbCoverageTable.controller';
import service from './ngbCoverageTable.service';
import ngbCovewrageTableActions from './ngbCoverageTableActions';

export default angular.module('ngbCoverageTable', [ngbCovewrageTableActions])
    .controller(controller.UID, controller)
    .component('ngbCoverageTable', ngbCoverageTable)
    .service('ngbCoverageTableService', service.instance)
    .name;
