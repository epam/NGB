import angular from 'angular';

import './ngbGenesTableFilter.scss';

import component from './ngbGenesTableFilter.component';
import controller from './ngbGenesTableFilter.controller';

export default angular.module('ngbGenesTableFilter', [])
    .controller(controller.UID, controller)
    .component('ngbGenesTableFilter', component)
    .name;
