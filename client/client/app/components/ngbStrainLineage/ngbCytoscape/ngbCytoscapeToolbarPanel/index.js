import angular from 'angular';
import component from './ngbCytoscapeToolbarPanel.component';

import controller from './ngbCytoscapeToolbarPanel.controller';
import './ngbCytoscapeToolbarPanel.scss';

export default angular.module('ngbCytoscapeToolbarPanelModule',[])
    .controller(controller.UID, controller)
    .component('ngbCytoscapeToolbarPanel', component)
    .name;
