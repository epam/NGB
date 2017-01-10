import angular from 'angular';

// Import Style

import ngbBookmarksPanel from './ngbBookmarksPanel.component' ;
import ngbBookmarksTable from './ngbBookmarksTable';

// Import external modules
export default angular.module('ngbBookmarksPanelComponent', [ngbBookmarksTable])
    .component('ngbBookmarksPanel', ngbBookmarksPanel)
    .name;