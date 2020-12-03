import './ngbTracksSelectionMenu.scss';

import angular from 'angular';

import component from './ngbTracksSelectionMenu.component';
import controller from './ngbTracksSelectionMenu.controller';

export default angular.module('ngbTracksSelectionMenu', [])
    .controller(controller.UID, controller)
    .component('ngbTracksSelectionMenu', component)
    .name;
