import angular from 'angular';

import './ngbGenomicsPanel.scss';

import component from './ngbGenomicsPanel.component';
import controller from './ngbGenomicsPanel.controller';
import service from './ngbGenomicsPanel.service';

import ngbGenomicsAlignment from './ngbGenomicsAlignment';
import ngbGenomicsTable from './ngbGenomicsTable';
import ngbGenomicsParasiteTable from './ngbGenomicsParasiteTable';

export default angular
    .module('ngbGenomicsPanel', [ngbGenomicsAlignment, ngbGenomicsTable, ngbGenomicsParasiteTable])
    .controller(controller.UID, controller)
    .component('ngbGenomicsPanel', component)
    .service('ngbGenomicsPanelService', service.instance)
    .name;
