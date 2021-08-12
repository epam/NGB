import angular from 'angular';

import './ngbMotifsTable.scss';

import ngbMotifsTable from './ngbMotifsTable.component';
import controller from './ngbMotifsTable.controller';
import service from './ngbMotifsTable.service';

import ngbMotifsPanelService from '../ngbMotifsPanel.service';
import ngbMotifsTablePagination from './ngbMotifsTablePagination';

export default angular.module('ngbMotifsTable', [ngbMotifsTablePagination])
    .service('ngbMotifsTableService', service.instance)
    .service('ngbMotifsPanelService', ngbMotifsPanelService.instance)
    .controller(controller.UID, controller)
    .component('ngbMotifsTable', ngbMotifsTable)
    .name;
