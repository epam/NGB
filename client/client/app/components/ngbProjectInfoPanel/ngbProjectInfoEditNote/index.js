import './ngbProjectInfoEditNote.scss';

import angular from 'angular';

import component from './ngbProjectInfoEditNote.component';
import controller from './ngbProjectInfoEditNote.controller';

export default angular.module('ngbProjectInfoEditNote', [])
    .controller(controller.UID, controller)
    .component('ngbProjectInfoEditNote', component)
    .name;
