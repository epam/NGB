import angular from 'angular';

import './ngbTargetsFormActions.scss';

import component from './ngbTargetsFormActions.component';
import controller from './ngbTargetsFormActions.controller';

export default angular.module('ngbTargetsFormActions', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsFormActions', component)
    .name;
