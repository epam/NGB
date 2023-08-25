import angular from 'angular';

import './ngbGenomicsPanel.scss';

import component from './ngbGenomicsPanel.component';
import controller from './ngbGenomicsPanel.controller';
import service from './ngbGenomicsPanel.service';

import ngbGenomicsAlignment from './ngbGenomicsAlignment';

export default angular
    .module('ngbGenomicsPanel', [ngbGenomicsAlignment])
    .controller(controller.UID, controller)
    .component('ngbGenomicsPanel', component)
    .service('ngbGenomicsPanelService', service.instance)
    .name;
