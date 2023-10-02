import angular from 'angular';

import './ngbDiseasesDrugsTableFilter.scss';

import component from './ngbDiseasesDrugsTableFilter.component';
import controller from './ngbDiseasesDrugsTableFilter.controller';

export default angular.module('ngbDiseasesDrugsTableFilter', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesDrugsTableFilter', component)
    .name;
