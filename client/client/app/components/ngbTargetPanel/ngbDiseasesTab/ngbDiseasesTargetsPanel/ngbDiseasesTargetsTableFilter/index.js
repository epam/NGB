import angular from 'angular';

import './ngbDiseasesTargetsTableFilter.scss';

import component from './ngbDiseasesTargetsTableFilter.component';
import controller from './ngbDiseasesTargetsTableFilter.controller';

export default angular.module('ngbDiseasesTargetsTableFilter', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTargetsTableFilter', component)
    .name;
