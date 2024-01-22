import angular from 'angular';

import './ngbTargetsTab.scss';

import component from './ngbTargetsTab.component';
import controller from './ngbTargetsTab.controller';
import service from './ngbTargetsTab.service';

import ngbTargetsTable from './ngbTargetsTable';
import ngbTargetsForm from './ngbTargetsForm';


export default angular
    .module('ngbTargetsTab', [ngbTargetsTable, ngbTargetsForm])
    .controller(controller.UID, controller)
    .component('ngbTargetsTab', component)
    .service('ngbTargetsTabService', service.instance)
    .name;
