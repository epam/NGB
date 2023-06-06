import angular from 'angular';

import './ngbIdentificationsTab.scss';

import component from './ngbIdentificationsTab.component';
import controller from './ngbIdentificationsTab.controller';

export default angular
    .module('ngbIdentificationsTab', [])
    .controller(controller.UID, controller)
    .component('ngbIdentificationsTab', component)
    .name;
