import './ngbBookmarksTableColumn.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbBookmarksTableColumn.component';
import controller from './ngbBookmarksTableColumn.controller';


// Import external modules
export default angular.module('ngbBookmarksTableColumnComponent', [])
    .controller(controller.UID, controller)
    .component('ngbBookmarksTableColumn', component)
    .name;
