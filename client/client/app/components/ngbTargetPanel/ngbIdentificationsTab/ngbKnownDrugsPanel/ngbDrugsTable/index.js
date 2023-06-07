import angular from 'angular';

import './ngbDrugsTable.scss';

import component from './ngbDrugsTable.component';
import controller from './ngbDrugsTable.controller';

export default angular
    .module('ngbDrugsTable', [])
    .controller(controller.UID, controller)
    .component('ngbDrugsTable', component)
    .name;
