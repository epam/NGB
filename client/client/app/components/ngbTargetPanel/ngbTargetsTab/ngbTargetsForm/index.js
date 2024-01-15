import angular from 'angular';

import './ngbTargetsForm.scss';

import component from './ngbTargetsForm.component';
import controller from './ngbTargetsForm.controller';

import ngbTargetGenesTable from './ngbTargetGenesTable';
import ngbTargetGenesList from './ngbTargetGenesTable/ngbTargetGenesTableCells/ngbTargetGenesList';

import ngbTargetsFormActions from './ngbTargetsFormActions';

export default angular
    .module('ngbTargetsForm', [ngbTargetGenesTable, ngbTargetGenesList, ngbTargetsFormActions])
    .controller(controller.UID, controller)
    .component('ngbTargetsForm', component)
    .name;
