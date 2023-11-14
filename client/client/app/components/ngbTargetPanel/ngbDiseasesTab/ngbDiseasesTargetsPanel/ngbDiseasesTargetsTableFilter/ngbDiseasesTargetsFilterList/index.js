import angular from 'angular';

import './ngbDiseasesTargetsFilterList.scss';

import component from './ngbDiseasesTargetsFilterList.component';
import controller from './ngbDiseasesTargetsFilterList.controller';

export default angular.module('ngbDiseasesTargetsFilterList', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTargetsFilterList', component)
    .name;
