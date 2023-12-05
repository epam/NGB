import angular from 'angular';

import './ngbPatentsChemicalTable.scss';

import component from './ngbPatentsChemicalTable.component';
import controller from './ngbPatentsChemicalTable.controller';

import ngbProteinTablePagination from './ngbProteinTablePagination';

export default angular
    .module('ngbPatentsChemicalTable', [ngbProteinTablePagination])
    .controller(controller.UID, controller)
    .component('ngbPatentsChemicalTable', component)
    .name;