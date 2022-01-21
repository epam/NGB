import angular from 'angular';

import './ngbMotifsParamsTable.scss';

import ngbMotifsParamsTable from './ngbMotifsParamsTable.component';
import ngbMotifsParamsTableController from './ngbMotifsParamsTable.controller';

import ngbMotifsPanelService from '../ngbMotifsPanel.service';

export default angular.module('ngbMotifsParamsTable', [])
    .service('ngbMotifsPanelService', ngbMotifsPanelService.instance)
    .controller(ngbMotifsParamsTableController.UID, ngbMotifsParamsTableController)
    .component('ngbMotifsParamsTable', ngbMotifsParamsTable)
    .name;
