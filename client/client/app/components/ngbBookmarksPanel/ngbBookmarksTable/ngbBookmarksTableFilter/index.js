import './ngbBookmarksTableFilter.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbBookmarksTableFilter.component';
import controller from './ngbBookmarksTableFilter.controller';
import filterInput from './ngbBookmarksFilterInput';
import filterList from './ngbBookmarksFilterList';

// Import external modules
export default angular.module('ngbBookmarksTableFilter', [filterInput, filterList])
    .controller(controller.UID, controller)
    .component('ngbBookmarksTableFilter', component)
    .name;
