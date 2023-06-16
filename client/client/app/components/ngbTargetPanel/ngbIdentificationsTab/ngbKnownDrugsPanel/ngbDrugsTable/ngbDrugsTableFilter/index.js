import angular from 'angular';

import './ngbDrugsTableFilter.scss';

import component from './ngbDrugsTableFilter.component';
import controller from './ngbDrugsTableFilter.controller';

export default angular.module('ngbDrugsTableFilter', [])
    .controller(controller.UID, controller)
    .component('ngbDrugsTableFilter', component)
    .name;
