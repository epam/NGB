import angular from 'angular';

import './ngbSequencesPanel.scss';

import component from './ngbSequencesPanel.component';
import controller from './ngbSequencesPanel.controller';
import service from './ngbSequencesPanel.service';

import ngbSequencesTable from './ngbSequencesTable';

export default angular
    .module('ngbSequencesPanel', [ngbSequencesTable])
    .controller(controller.UID, controller)
    .component('ngbSequencesPanel', component)
    .service('ngbSequencesPanelService', service.instance)
    .name;
