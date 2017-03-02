import './ngbOpenFile.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbOpenFile.controller';
import component from './ngbOpenFile.component';
import ngbOpenFileFromUrl from './ngbOpenFileFromUrl';
import ngbOpenFileFromNGBServer from './ngbOpenFileFromNGBServer';

export default angular.module('ngbOpenFile', [ngbOpenFileFromUrl, ngbOpenFileFromNGBServer])
    .controller(controller.UID, controller)
    .component('ngbOpenFile',component)
    .name;
