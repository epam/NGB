import './ngbTargetsTableFilter.scss';

import angular from 'angular';

import component from './ngbTargetsTableFilter.component';
import controller from './ngbTargetsTableFilter.controller';

import ngbTargetsFilterList from './ngbTargetsFilterList';
import ngbTargetsFilterInput from './ngbTargetsFilterInput';

export default angular.module('ngbTargetsTableFilter', [ngbTargetsFilterList, ngbTargetsFilterInput])
    .controller(controller.UID, controller)
    .component('ngbTargetsTableFilter', component)
    .name;
