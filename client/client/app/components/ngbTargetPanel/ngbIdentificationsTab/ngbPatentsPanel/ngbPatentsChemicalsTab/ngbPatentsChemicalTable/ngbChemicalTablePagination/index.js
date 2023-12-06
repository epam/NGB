import angular from 'angular';

import './ngbChemicalTablePagination.scss';

import component from './ngbChemicalTablePagination.component';
import controller from './ngbChemicalTablePagination.controller';

export default angular
    .module('ngbChemicalTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbChemicalTablePagination', component)
    .name;
