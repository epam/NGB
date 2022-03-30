// Import Style
import angular from 'angular';
import component from './ngbInternalPathwaysTable.component';
import controller from './ngbInternalPathwaysTable.controller';
import './ngbInternalPathwaysTable.scss';

import service from './ngbInternalPathwaysTable.service';

import ngbInternalPathwaysTableFilter from './ngbInternalPathwaysTableFilter';

export default angular
    .module('ngbInternalPathwaysTable', [ngbInternalPathwaysTableFilter])
    .service('ngbInternalPathwaysTableService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbInternalPathwaysTable', component)
    .name;
