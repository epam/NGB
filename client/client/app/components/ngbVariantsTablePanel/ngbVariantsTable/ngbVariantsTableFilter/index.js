import './ngbVariantsTableFilter.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbVariantsTableFilter.controller';
import component from './ngbVariantsTableFilter.component';
import filterList from './ngbVariantsFilterList';
import filterCheckbox from './ngbVariantsFilterCheckbox';
import filterRange from './ngbVariantsFilterRange';
import filterInput from './ngbVariantsFilterInput';

// Import external modules
export default angular.module('ngbVariantsTableFilter', [filterList, filterCheckbox, filterRange, filterInput])
    .controller(controller.UID, controller)
    .component('ngbVariantsTableFilter', component)
    .name;