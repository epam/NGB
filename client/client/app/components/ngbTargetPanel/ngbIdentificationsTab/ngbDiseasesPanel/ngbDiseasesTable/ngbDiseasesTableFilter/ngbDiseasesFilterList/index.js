import angular from 'angular';

import './ngbDiseasesFilterList.scss';

import component from './ngbDiseasesFilterList.component';
import controller from './ngbDiseasesFilterList.controller';

export default angular.module('ngbDiseasesFilterList', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesFilterList', component)
    .name;
