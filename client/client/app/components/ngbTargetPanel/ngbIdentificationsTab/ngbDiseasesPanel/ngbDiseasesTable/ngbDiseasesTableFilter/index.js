import angular from 'angular';

import './ngbDiseasesTableFilter.scss';

import component from './ngbDiseasesTableFilter.component';
import controller from './ngbDiseasesTableFilter.controller';

import filterInput from './ngbDiseasesFilterInput';
import filterList from './ngbDiseasesFilterList';

export default angular.module('ngbDiseasesTableFilter', [filterList, filterInput])
    .controller(controller.UID, controller)
    .component('ngbDiseasesTableFilter', component)
    .name;
