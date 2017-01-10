// Import Style
import './ngbBookmarksTable.scss';

import angular from 'angular';
import uiGrid from '../../../compat/uiGrid';

// Import internal modules
import component from './ngbBookmarksTable.component';
import controller from  './ngbBookmarksTable.controller';
import service from './ngbBookmarksTable.service';

// Import external modules
import bookmarkDataService  from '../../../../dataServices/angular-module';


export default angular.module('ngbBookmarksTableComponent', [bookmarkDataService, uiGrid])
    .service('bookmarksTableService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbBookmarksTable', component)
    .name;