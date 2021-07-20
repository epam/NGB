// Import Style
import './ngbTrackNotification.scss';

// Import internal modules
import angular from 'angular';

import component from './ngbTrackNotification.component';
import controller from './ngbTrackNotification.controller';

export default angular.module('ngbTrackNotification', [])
    .controller(controller.UID, controller)
    .component('ngbTrackNotification', component)
    .name;
