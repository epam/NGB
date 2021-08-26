import './ngbBookmarksFilterInput.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbBookmarksFilterInput.component';
import controller from './ngbBookmarksFilterInput.controller';

// Import external modules
export default angular.module('ngbBookmarksFilterInput', [])
    .controller(controller.UID, controller)
    .component('ngbBookmarksFilterInput', component)
    .name;
