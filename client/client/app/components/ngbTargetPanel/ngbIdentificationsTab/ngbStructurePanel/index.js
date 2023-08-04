import angular from 'angular';

import './ngbStructurePanel.scss';

import component from './ngbStructurePanel.component';
import controller from './ngbStructurePanel.controller';

export default angular
    .module('ngbStructurePanel', [])
    .controller(controller.UID, controller)
    .component('ngbStructurePanel', component)
    .name;
