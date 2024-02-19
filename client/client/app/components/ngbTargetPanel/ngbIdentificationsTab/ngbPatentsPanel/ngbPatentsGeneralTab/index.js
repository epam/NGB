import angular from 'angular';

import './ngbPatentsGeneralTab.scss';

import component from './ngbPatentsGeneralTab.component';
import controller from './ngbPatentsGeneralTab.controller';
import service from './ngbPatentsGeneralTab.service';

export default angular
    .module('ngbPatentsGeneralTab', [])
    .controller(controller.UID, controller)
    .component('ngbPatentsGeneralTab', component)
    .service('ngbPatentsGeneralTabService', service.instance)
    .name;