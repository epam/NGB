import angular from 'angular';
import filterList from './ngbInternalPathwaysFilterList';

// Import internal modules
import component from './ngbInternalPathwaysTableFilter.component';
import controller from './ngbInternalPathwaysTableFilter.controller';
import './ngbInternalPathwaysTableFilter.scss';

// Import external modules
export default angular.module('ngbInternalPathwaysTableFilter', [filterList])
    .controller(controller.UID, controller)
    .component('ngbInternalPathwaysTableFilter', component)
    .name;
