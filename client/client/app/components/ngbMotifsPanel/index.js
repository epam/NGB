import angular from 'angular';

import './ngbMotifsPanel.scss';

import run from './ngbMotifsDialog.run';
import ngbMotifsPanel from './ngbMotifsPanel.component';
import ngbMotifsPanelController from './ngbMotifsPanel.controller';
import ngbMotifsPanelService from './ngbMotifsPanel.service';

import ngbMotifsTable from './ngbMotifsTable';

export default angular.module('ngbMotifsPanel', [ngbMotifsTable])
    .controller(ngbMotifsPanelController.UID, ngbMotifsPanelController)
    .component('ngbMotifsPanel', ngbMotifsPanel)
    .service('ngbMotifsPanelService', ngbMotifsPanelService.instance)
    .run(run)
    .name;
