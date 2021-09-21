import './ngbProjectInfoSections.scss';

import angular from 'angular';

import component from './ngbProjectInfoSections.component';
import controller from './ngbProjectInfoSections.controller';

export default angular.module('ngbProjectInfoSections', [])
    .controller(controller.UID, controller)
    .component('ngbProjectInfoSections', component)
    .name;
