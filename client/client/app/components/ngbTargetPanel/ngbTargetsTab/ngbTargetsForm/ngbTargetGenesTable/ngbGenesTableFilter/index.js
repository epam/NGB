import angular from 'angular';

import './ngbGenesTableFilter.scss';

import component from './ngbGenesTableFilter.component';
import controller from './ngbGenesTableFilter.controller';

import ngbTargetGenesFilterList from './ngbTargetGenesFilterList';
import ngbTargetGenesFilterRange from './ngbTargetGenesFilterRange';
import ngbTargetGenesFilterInput from './ngbTargetGenesFilterInput';

export default angular.module('ngbGenesTableFilter', [
    ngbTargetGenesFilterInput,
    ngbTargetGenesFilterList,
    ngbTargetGenesFilterRange,
])
    .controller(controller.UID, controller)
    .component('ngbGenesTableFilter', component)
    .name;
