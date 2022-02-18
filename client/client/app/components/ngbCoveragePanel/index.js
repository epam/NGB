import angular from 'angular';

import './ngbCoveragePanel.scss';

import ngbCoveragePanel from './ngbCoveragePanel.component';
import controller from './ngbCoveragePanel.controller';
import service from './ngbCoveragePanel.service';
import ngbCoverageTable from './ngbCoverageTable';

export default angular.module('ngbCoveragePanel', [ngbCoverageTable])
    .controller(controller.UID, controller)
    .component('ngbCoveragePanel', ngbCoveragePanel)
    .service('ngbCoveragePanelService', service.instance)
    .name;
