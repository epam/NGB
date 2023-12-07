import angular from 'angular';

import './ngbDiseasesDrugsPanel.scss';

import component from './ngbDiseasesDrugsPanel.component';
import controller from './ngbDiseasesDrugsPanel.controller';
import service from './ngbDiseasesDrugsPanel.service';

import ngbDiseasesDrugsTablePagination from './ngbDiseasesDrugsTablePagination';
import ngbDiseasesDrugsTableFilter from './ngbDiseasesDrugsTableFilter';

export default angular
    .module('ngbDiseasesDrugsPanel', [ngbDiseasesDrugsTablePagination, ngbDiseasesDrugsTableFilter])
    .controller(controller.UID, controller)
    .component('ngbDiseasesDrugsPanel', component)
    .service('ngbDiseasesDrugsPanelService', service.instance)
    .name;