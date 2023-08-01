import angular from 'angular';

import './ngbSequencesPanel.scss';

import component from './ngbSequencesPanel.component';
import controller from './ngbSequencesPanel.controller';
// import service from './ngbSequencesPanel.service';

export default angular
    .module('ngbSequencesPanel', [])
    .controller(controller.UID, controller)
    .component('ngbSequencesPanel', component)
    // .service('ngbSequencesPanelService', service.instance)
    .name;
