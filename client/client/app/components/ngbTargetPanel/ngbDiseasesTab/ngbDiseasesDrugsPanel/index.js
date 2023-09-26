import angular from 'angular';

import './ngbDiseasesDrugsPanel.scss';

import component from './ngbDiseasesDrugsPanel.component';
import controller from './ngbDiseasesDrugsPanel.controller';
import service from './ngbDiseasesDrugsPanel.service';

import ngbDrugsTablePagination from './ngbDrugsTablePagination';

export default angular
    .module('ngbDiseasesDrugsPanel', [ngbDrugsTablePagination])
    .controller(controller.UID, controller)
    .component('ngbDiseasesDrugsPanel', component)
    .service('ngbDiseasesDrugsPanelService', service.instance)
    .name;