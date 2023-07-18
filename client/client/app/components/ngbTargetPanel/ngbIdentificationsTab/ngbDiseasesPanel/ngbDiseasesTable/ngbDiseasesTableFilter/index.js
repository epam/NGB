import angular from 'angular';

import './ngbDiseasesTableFilter.scss';

import component from './ngbDiseasesTableFilter.component';
import controller from './ngbDiseasesTableFilter.controller';

export default angular.module('ngbDiseasesTableFilter', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTableFilter', component)
    .name;
