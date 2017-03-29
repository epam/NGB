import './ngbVariantsFilterList.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbVariantsFilterList.controller';
import component from './ngbVariantsFilterList.component';
import scroller from './ngbVariantsFilterList.scroller';

// Import external modules
export default angular.module('ngbVariantsFilterList', [])
    .directive('preventParentScroll', scroller)
    .controller(controller.UID, controller)
    .component('ngbVariantsFilterList', component)
    .name;