import angular from 'angular';

import './ngbPatentsSequencesTab.scss';

import component from './ngbPatentsSequencesTab.component';
import controller from './ngbPatentsSequencesTab.controller';
import service from './ngbPatentsSequencesTab.service';

export default angular
    .module('ngbPatentsSequencesTab', [])
    .controller(controller.UID, controller)
    .component('ngbPatentsSequencesTab', component)
    .service('ngbPatentsSequencesTabService', service.instance)
    .name;