import angular from 'angular';

import './ngbKnownDrugsPanel.scss';

import component from './ngbKnownDrugsPanel.component';
import controller from './ngbKnownDrugsPanel.controller';
import service from './ngbKnownDrugsPanel.service';

import ngbDrugsTable from './ngbDrugsTable';

export default angular
    .module('ngbKnownDrugsPanel', [ngbDrugsTable])
    .controller(controller.UID, controller)
    .component('ngbKnownDrugsPanel', component)
    .service('ngbKnownDrugsPanelService', service.instance)
    .name;
