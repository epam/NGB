import './ngbBrowserToolbarPanel.scss';

import angular from 'angular';

import controller from './ngbBrowserToolbarPanel.controller';
import component from './ngbBrowserToolbarPanel.component';

import ngbBookmark from './ngbBookmark';

export default angular.module('ngbBrowserToolbarPanelModule', [ngbBookmark])
    .controller(controller.UID, controller)
    .component('ngbBrowserToolbarPanel', component)
    .name;
