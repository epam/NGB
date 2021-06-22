import './ngbProjectInfoSections.scss';

import angular from 'angular';

import component from './ngbProjectInfoSections.component';
import controller from './ngbProjectInfoSections.controller';
import service from './ngbProjectInfoSections.service';

export default angular.module('ngbProjectInfoSections', [])
    .controller(controller.UID, controller)
    .component('ngbProjectInfoSections', component)
    .service('ngbProjectInfoService', service)
    .name;
