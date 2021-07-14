import './ngbVariantsFilterList.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbVariantsFilterList.component';
import controller from './ngbVariantsFilterList.controller';
import scroller from './../../../../../shared/filter/filterList/ngbFilterList.scroller';

// Import external modules
export default angular.module('ngbVariantsFilterList', [])
    .directive('preventParentScroll', scroller)
    .controller(controller.UID, controller)
    .component('ngbVariantsFilterList', component)
    .name;
