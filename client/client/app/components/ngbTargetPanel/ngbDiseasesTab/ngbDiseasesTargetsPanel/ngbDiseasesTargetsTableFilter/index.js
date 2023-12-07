import angular from 'angular';

import './ngbDiseasesTargetsTableFilter.scss';

import component from './ngbDiseasesTargetsTableFilter.component';
import controller from './ngbDiseasesTargetsTableFilter.controller';

import filterInput from './ngbDiseasesTargetsFilterInput';
import filterList from './ngbDiseasesTargetsFilterList';

export default angular.module('ngbDiseasesTargetsTableFilter', [filterList, filterInput])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTargetsTableFilter', component)
    .name;
