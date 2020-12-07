import angular from 'angular';

import component from './ngbTrackResizePreference.component';
import controller from './ngbTrackResizePreference.controller';
import run from './ngbTrackResizePreference.run';

export default angular.module('ngbTrackResizePreference', [])
    .controller(controller.UID, controller)
    .component('ngbTrackResizePreference', component)
    .run(run)
    .name;