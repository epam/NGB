import angular from 'angular';

import './ngbPatentsProteinTable.scss';

import component from './ngbPatentsProteinTable.component';
import controller from './ngbPatentsProteinTable.controller';

export default angular
    .module('ngbPatentsProteinTable', [])
    .controller(controller.UID, controller)
    .component('ngbPatentsProteinTable', component)
    .name;