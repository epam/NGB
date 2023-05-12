import angular from 'angular';

import './ngbTargetsTableActions.scss';

import component from './ngbTargetsTableActions.component';
import controller from './ngbTargetsTableActions.controller';

export default angular.module('ngbTargetsTableActions', [])
    .controller(controller.UID, controller)
    .component('ngbTargetsTableActions', component)
    .name;
