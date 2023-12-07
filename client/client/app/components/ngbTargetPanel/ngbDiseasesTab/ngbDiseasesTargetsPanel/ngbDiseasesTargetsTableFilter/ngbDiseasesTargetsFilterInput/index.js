import angular from 'angular';

import component from './ngbDiseasesTargetsFilterInput.component';
import controller from './ngbDiseasesTargetsFilterInput.controller';

export default angular.module('ngbDiseasesTargetsFilterInput', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTargetsFilterInput', component)
    .name;
