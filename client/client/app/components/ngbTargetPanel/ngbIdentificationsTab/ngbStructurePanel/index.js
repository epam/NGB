import angular from 'angular';

import './ngbStructurePanel.scss';

import component from './ngbStructurePanel.component';
import controller from './ngbStructurePanel.controller';
import service from './ngbStructurePanel.service';

import ngbStructureTable from './ngbStructureTable';

export default angular
    .module('ngbStructurePanel', [ngbStructureTable])
    .controller(controller.UID, controller)
    .component('ngbStructurePanel', component)
    .service('ngbStructurePanelService', service.instance)
    .name;
