import './ngbBlastSearchResult.scss';
import angular from 'angular';

// Import internal modules
import component from './ngbBlastSearchResult.component';
import controller from './ngbBlastSearchResult.controller';


// Import external modules
export default angular.module('ngbBlastSearchResult', [])
    .controller(controller.UID, controller)
    .component('ngbBlastSearchResult', component)
    .name;
