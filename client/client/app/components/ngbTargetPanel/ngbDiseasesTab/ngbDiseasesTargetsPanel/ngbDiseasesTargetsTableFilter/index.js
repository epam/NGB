import angular from 'angular';

import './ngbDiseasesTargetsTableFilter.scss';

import component from './ngbDiseasesTargetsTableFilter.component';
import controller from './ngbDiseasesTargetsTableFilter.controller';

import filterInput from './ngbTargetsFilterInput';
import filterList from './ngbTargetsFilterList';

export default angular.module('ngbDiseasesTargetsTableFilter', [filterList, filterInput])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTargetsTableFilter', component)
    .name;
