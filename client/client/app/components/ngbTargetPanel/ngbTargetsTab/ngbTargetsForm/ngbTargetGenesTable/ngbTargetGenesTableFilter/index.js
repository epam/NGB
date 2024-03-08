import angular from 'angular';

import './ngbTargetGenesTableFilter.scss';

import component from './ngbTargetGenesTableFilter.component';
import controller from './ngbTargetGenesTableFilter.controller';

import ngbTargetGenesFilterList from './ngbTargetGenesFilterList';
import ngbTargetGenesFilterRange from './ngbTargetGenesFilterRange';
import ngbTargetGenesFilterInput from './ngbTargetGenesFilterInput';

export default angular.module('ngbTargetGenesTableFilter', [
    ngbTargetGenesFilterInput,
    ngbTargetGenesFilterList,
    ngbTargetGenesFilterRange,
])
    .controller(controller.UID, controller)
    .component('ngbTargetGenesTableFilter', component)
    .name;
