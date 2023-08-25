import angular from 'angular';

import './ngbGenomicsPanel.scss';

import component from './ngbGenomicsPanel.component';
import controller from './ngbGenomicsPanel.controller';

export default angular
    .module('ngbGenomicsPanel', [])
    .controller(controller.UID, controller)
    .component('ngbGenomicsPanel', component)
    .name;
