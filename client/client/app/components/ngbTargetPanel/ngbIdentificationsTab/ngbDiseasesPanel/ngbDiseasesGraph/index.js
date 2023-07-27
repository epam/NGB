import angular from 'angular';

import component from './ngbDiseasesGraph.component';
import controller from './ngbDiseasesGraph.controller';

export default angular
    .module('ngbDiseasesGraph', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesGraph', component)
    .name;
