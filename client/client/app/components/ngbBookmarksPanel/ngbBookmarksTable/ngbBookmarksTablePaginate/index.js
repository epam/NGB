import angular from 'angular';
import './ngbBookmarksTablePaginate.scss';

// Import internal modules
import component from './ngbBookmarksTablePaginate.component';
import controller from './ngbBookmarksTablePaginate.controller';


// Import external modules
export default angular.module('ngbBookmarksTablePaginate', [])
    .controller(controller.UID, controller)
    .component('ngbBookmarksTablePaginate', component)
    .name;
