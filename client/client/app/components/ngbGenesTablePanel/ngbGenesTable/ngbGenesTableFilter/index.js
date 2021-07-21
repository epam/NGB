import './ngbGenesTableFilter.scss';

import angular from 'angular';

// Import internal modules
import component from './ngbGenesTableFilter.component';
import controller from './ngbGenesTableFilter.controller';
import filterInput from './ngbGenesFilterInput';
import filterList from './ngbGenesFilterList';
import filterRange from './ngbGenesFilterRange';

// Import external modules
export default angular.module('ngbGenesTableFilter', [filterRange, filterInput, filterList])
    .controller(controller.UID, controller)
    .component('ngbGenesTableFilter', component)
    .name;
