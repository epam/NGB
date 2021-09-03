import './ngbBookmarksFilterList.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbBookmarksFilterList.component';
import controller from './ngbBookmarksFilterList.controller';
import scroller from '../../../../../shared/filter/filterList/ngbFilterList.scroller';

// Import external modules
export default angular.module('ngbBookmarksFilterList', [])
    .directive('preventParentScroll', scroller)
    .controller(controller.UID, controller)
    .component('ngbBookmarksFilterList', component)
    .name;
