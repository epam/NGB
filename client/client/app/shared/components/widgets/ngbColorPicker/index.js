import './ngbColorPicker.scss';

import angular from 'angular';

import component from './ngbColorPicker.component';
import controller from './ngbColorPicker.controller';

export default angular.module('ngbColorPicker', [])
    .controller(controller.UID, controller)
    .component('ngbColorPicker', component)
    .name;
