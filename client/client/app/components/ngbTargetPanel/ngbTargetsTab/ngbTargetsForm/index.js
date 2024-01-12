import angular from 'angular';

import './ngbTargetsForm.scss';

import component from './ngbTargetsForm.component';
import controller from './ngbTargetsForm.controller';

import ngbTargetGenesTable from './ngbTargetGenesTable';
import ngbTargetGenesList from './ngbTargetGenesTable/ngbTargetGenesTableCells/ngbTargetGenesList';

export default angular
    .module('ngbTargetsForm', [ngbTargetGenesTable, ngbTargetGenesList])
    .controller(controller.UID, controller)
    .component('ngbTargetsForm', component)
    .name;
