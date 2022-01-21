import angular from 'angular';

import './ngbMotifsDialog.scss';

import run from './ngbMotifsDialog.run';
import ngbMotifsPanel from './ngbMotifsPanel.component';
import ngbMotifsPanelController from './ngbMotifsPanel.controller';
import ngbMotifsPanelService from './ngbMotifsPanel.service';
import motifSequenceValidation from './motif.sequence.directive';

import ngbMotifsResultsTable from './ngbMotifsResultsTable';
import ngbMotifsParamsTable from './ngbMotifsParamsTable';

export default angular
    .module('ngbMotifsPanel', [ngbMotifsResultsTable, ngbMotifsParamsTable])
    .directive('motifSequence', motifSequenceValidation)
    .controller(ngbMotifsPanelController.UID, ngbMotifsPanelController)
    .component('ngbMotifsPanel', ngbMotifsPanel)
    .service('ngbMotifsPanelService', ngbMotifsPanelService.instance)
    .run(run)
    .name;
