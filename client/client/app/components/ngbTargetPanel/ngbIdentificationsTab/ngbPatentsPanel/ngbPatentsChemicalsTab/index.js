import angular from 'angular';

import './ngbPatentsChemicalsTab.scss';

import component from './ngbPatentsChemicalsTab.component';
import controller from './ngbPatentsChemicalsTab.controller';
import service from './ngbPatentsChemicalsTab.service';

import ngbPatentsChemicalTable from './ngbPatentsChemicalTable';

export default angular
    .module('ngbPatentsChemicalsTab', [ngbPatentsChemicalTable])
    .controller(controller.UID, controller)
    .component('ngbPatentsChemicalsTab', component)
    .service('ngbPatentsChemicalsTabService', service.instance)
    .name;