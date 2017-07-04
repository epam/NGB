import './ngbGenomeAnnotations.scss';

import angular from 'angular';

import controller from './ngbGenomeAnnotations.controller';
import component from './ngbGenomeAnnotations.component';

export default angular.module('ngbGenomeAnnotations', [])
    .controller(controller.UID, controller)
    .component('ngbGenomeAnnotations', component)
    .name;
