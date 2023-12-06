import angular from 'angular';

import './ngbPatentsChemicalTable.scss';

import component from './ngbPatentsChemicalTable.component';
import controller from './ngbPatentsChemicalTable.controller';

import ngbChemicalTablePagination from './ngbChemicalTablePagination';

export default angular
    .module('ngbPatentsChemicalTable', [ngbChemicalTablePagination])
    .controller(controller.UID, controller)
    .component('ngbPatentsChemicalTable', component)
    .name;