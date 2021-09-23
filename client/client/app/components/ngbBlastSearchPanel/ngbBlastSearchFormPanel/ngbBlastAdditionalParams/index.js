import angular from 'angular';

// Import internal modules
import component from './ngbBlastAdditionalParams.component';
import controller from './ngbBlastAdditionalParams.controller';
import './ngbBlastAdditionalParams.scss';

export default angular.module('ngbBlastAdditionalParams', [])
    .controller(controller.UID, controller)
    .component('ngbBlastAdditionalParams', component)
    .name;
