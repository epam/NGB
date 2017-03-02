import './ngbBrowserToolbarPanel.scss';

import angular from 'angular';

import controller from './ngbBrowserToolbarPanel.controller';
import component from './ngbBrowserToolbarPanel.component';

export default angular.module('ngbBrowserToolbarPanelModule',[])
    .controller(controller.UID, controller)
    .component('ngbBrowserToolbarPanel', component)
    .name;
