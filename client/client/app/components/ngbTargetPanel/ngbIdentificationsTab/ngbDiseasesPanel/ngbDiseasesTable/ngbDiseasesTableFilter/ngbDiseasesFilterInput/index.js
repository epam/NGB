import angular from 'angular';

import component from './ngbDiseasesFilterInput.component';
import controller from './ngbDiseasesFilterInput.controller';

export default angular.module('ngbDiseasesFilterInput', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesFilterInput', component)
    .name;
