import './ngbCloseAllTracks.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbCloseAllTracks.component';
import controller from './ngbCloseAllTracks.controller';

// Import external modules
export default angular.module('ngbCloseAllTracks', [])
    .controller(controller.UID, controller)
    .component('ngbCloseAllTracks', component)
    .name;