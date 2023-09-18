import angular from 'angular';

import './ngbDiseasesTab.scss';

import component from './ngbDiseasesTab.component';
import controller from './ngbDiseasesTab.controller';

export default angular
    .module('ngbDiseasesTab', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTab', component)
    .name;
