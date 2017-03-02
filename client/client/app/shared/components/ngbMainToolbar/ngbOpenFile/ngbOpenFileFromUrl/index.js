import './ngbOpenFileFromUrl.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbOpenFileFromUrl.controller';
import component from './ngbOpenFileFromUrl.component';


export default angular.module('ngbOpenFileFromUrl', [])
    .controller(controller.UID, controller)
    .component('ngbOpenFileFromUrl',component)
    .name;
