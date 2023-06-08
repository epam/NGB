import angular from 'angular';

import './ngbDiseasesTab.scss';

import component from './ngbDiseasesTab.component';
import controller from './ngbDiseasesTab.controller';

import ngbDiseasesTable from './ngbDiseasesTable';

export default angular
    .module('ngbDiseasesTab', [ngbDiseasesTable])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTab', component)
    .name;
