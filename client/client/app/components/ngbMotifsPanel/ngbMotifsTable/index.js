import angular from 'angular';

import './ngbMotifsTable.scss';

import ngbMotifsTable from './ngbMotifsTable.component';
import controller from './ngbMotifsTable.controller';
import service from './ngbMotifsTable.service';

import ngbMotifsPanelService from '../ngbMotifsPanel.service';

export default angular.module('ngbMotifsTable', [])
    .service('ngbMotifsTableService', service.instance)
    .service('ngbMotifsPanelService', ngbMotifsPanelService.instance)
    .controller(controller.UID, controller)
    .component('ngbMotifsTable', ngbMotifsTable)
    .name;
