import angular from 'angular';
import scroller from '../../../../../shared/filter/filterList/ngbFilterList.scroller';

// Import internal modules
import component from './ngbInternalPathwaysFilterList.component';
import controller from './ngbInternalPathwaysFilterList.controller';
import './ngbInternalPathwaysFilterList.scss';

// Import external modules
export default angular.module('ngbInternalPathwaysFilterList', [])
    .directive('preventParentScroll', scroller)
    .controller(controller.UID, controller)
    .component('ngbInternalPathwaysFilterList', component)
    .name;
