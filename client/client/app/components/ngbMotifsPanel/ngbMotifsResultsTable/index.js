import angular from 'angular';

import './ngbMotifsResultsTable.scss';

import ngbMotifsResultsTable from './ngbMotifsResultsTable.component';
import ngbMotifsResultsTableController from './ngbMotifsResultsTable.controller';

import ngbMotifsPanelService from '../ngbMotifsPanel.service';

export default angular
    .module('ngbMotifsResultsTable', [])
    .service('ngbMotifsPanelService', ngbMotifsPanelService.instance)
    .controller(ngbMotifsResultsTableController.UID, ngbMotifsResultsTableController)
    .component('ngbMotifsResultsTable', ngbMotifsResultsTable)
    .name;
