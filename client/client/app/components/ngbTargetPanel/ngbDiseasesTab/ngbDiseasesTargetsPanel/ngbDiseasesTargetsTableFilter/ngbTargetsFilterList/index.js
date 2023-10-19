import angular from 'angular';

import './ngbTargetsFilterList.scss';

import component from './ngbTargetsFilterList.component';
import controller from './ngbTargetsFilterList.controller';

export default angular.module('ngbDiseasesTargetsFilterList', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTargetsFilterList', component)
    .name;
