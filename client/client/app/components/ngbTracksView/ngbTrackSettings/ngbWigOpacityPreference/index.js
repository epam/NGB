import angular from 'angular';

import component from './ngbWigOpacityPreference.component';
import controller from './ngbWigOpacityPreference.controller';
import run from './ngbWigOpacityPreference.run';

export default angular.module('ngbWigOpacityPreference', [])
    .controller(controller.UID, controller)
    .component('ngbWigOpacityPreference', component)
    .run(run)
    .name;