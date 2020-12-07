import './ngbTracksSelection.scss';

import angular from 'angular';

import component from './ngbTracksSelection.component';
import controller from './ngbTracksSelection.controller';

export default angular.module('ngbTracksSelection', [])
    .controller(controller.UID, controller)
    .component('ngbTracksSelection', component)
    .name;
