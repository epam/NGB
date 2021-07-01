import './ngbGenesFilterList.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbGenesFilterList.component';
import controller from './ngbGenesFilterList.controller';
import scroller from './../../../../shared/filter/filterList/ngbFilterList.scroller';

// Import external modules
export default angular.module('ngbGenesFilterList', [])
    .directive('preventParentScroll', scroller)
    .controller(controller.UID, controller)
    .component('ngbGenesFilterList', component)
    .name;
