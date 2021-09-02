import angular from 'angular';

import './ngbMotifsTablePagination.scss';

import ngbMotifsPanelService from '../../ngbMotifsPanel.service';

import component from './ngbMotifsTablePagination.component';
import controller from './ngbMotifsTablePagination.controller';

export default angular.module('ngbMotifsTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbMotifsTablePagination', component)
    .service('ngbMotifsPanelService', ngbMotifsPanelService.instance)
    .name;
