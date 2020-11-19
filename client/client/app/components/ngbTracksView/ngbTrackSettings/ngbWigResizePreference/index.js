import angular from 'angular';

import component from './ngbWigResizePreference.component';
import controller from './ngbWigResizePreference.controller';
import run from './ngbWigResizePreference.run';

export default angular.module('ngbWigResizePreference', [])
    .controller(controller.UID, controller)
    .component('ngbWigResizePreference', component)
    .run(run)
    .name;