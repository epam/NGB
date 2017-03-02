import './ngbOpenFileFromNGBServer.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbOpenFileFromNGBServer.controller';
import component from './ngbOpenFileFromNGBServer.component';
import sizeFilter from './ngbOpenFileFromNGBServer.size.filter';

export default angular.module('ngbOpenFileFromNgbServer', [])
    .controller(controller.UID, controller)
    .component('ngbOpenFileFromNgbServer',component)
    .filter('sizeFilter', sizeFilter)
    .name;
