import angular from 'angular';

import './ngbKnownDrugsPanel.scss';

import component from './ngbKnownDrugsPanel.component';
import controller from './ngbKnownDrugsPanel.controller';

import ngbDrugsTable from './ngbDrugsTable';
import ngbDiseasesTab from './ngbDiseasesTab';

export default angular
    .module('ngbKnownDrugsPanel', [ngbDrugsTable, ngbDiseasesTab])
    .controller(controller.UID, controller)
    .component('ngbKnownDrugsPanel', component)
    .name;
