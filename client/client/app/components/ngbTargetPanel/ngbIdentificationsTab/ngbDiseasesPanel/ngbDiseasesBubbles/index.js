import angular from 'angular';

import component from './ngbDiseasesBubbles.component';
import controller from './ngbDiseasesBubbles.controller';

export default angular
    .module('ngbDiseasesBubbles', [])
    .controller(controller.UID, controller)
    .component('ngbDiseasesBubbles', component)
    .name;
