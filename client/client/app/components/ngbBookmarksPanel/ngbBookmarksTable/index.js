// Import Style
import './ngbBookmarksTable.scss';

import angular from 'angular';
import uiGrid from '../../../compat/uiGrid';

// Import internal modules
import component from './ngbBookmarksTable.component';
import controller from  './ngbBookmarksTable.controller';
import service from './ngbBookmarksTable.service';
import ngbBookmarksTableColumn from './ngbBookmarksTableColumn';
import ngbBookmarksTableFilter from './ngbBookmarksTableFilter';

// Import external modules
import bookmarkDataService  from '../../../../dataServices/angular-module';


export default angular.module('ngbBookmarksTableComponent', [ngbBookmarksTableColumn, ngbBookmarksTableFilter, bookmarkDataService, uiGrid])
    .service('ngbBookmarksTableService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbBookmarksTable', component)
    .name;
