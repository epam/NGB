import angular from 'angular';

import './ngbTargetsTab.scss';

import component from './ngbTargetsTab.component';
import controller from './ngbTargetsTab.controller';
import service from './ngbTargetsTab.service';

import ngbTargetsForm from './ngbTargetsForm';
import ngbTargetsTable from './ngbTargetsTable';

export default angular
    .module('ngbTargetsTab', [ngbTargetsTable, ngbTargetsForm])
    .controller(controller.UID, controller)
    .component('ngbTargetsTab', component)
    .service('ngbTargetsTabService', service.instance)
    .name;
