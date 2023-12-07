import angular from 'angular';

import './ngbDiseasesDrugsTablePagination.scss';

import component from './ngbDiseasesDrugsTablePagination.component';
import controller from './ngbDiseasesDrugsTablePagination.controller';

export default angular
    .module('ngbDiseasesDrugsTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesDrugsTablePagination', component)
    .name;
