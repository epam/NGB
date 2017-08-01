import angular from 'angular';
import './ngbVariantsLoadingIndicator.scss';

// Import internal modules
import component from './ngbVariantsLoadingIndicator.component';
import controller from './ngbVariantsLoadingIndicator.controller';

// Import external modules
export default angular.module('ngbVariantsLoadingIndicator', [])
    .controller(controller.UID, controller)
    .component('ngbVariantsLoadingIndicator', component)
    .name;