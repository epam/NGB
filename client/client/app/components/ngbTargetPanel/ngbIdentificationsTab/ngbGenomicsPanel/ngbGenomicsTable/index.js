import angular from 'angular';

import './ngbGenomicsTable.scss';

import component from './ngbGenomicsTable.component';
import controller from './ngbGenomicsTable.controller';

export default angular
    .module('ngbGenomicsTable', [])
    .controller(controller.UID, controller)
    .component('ngbGenomicsTable', component)
    .name;
