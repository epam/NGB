import angular from 'angular';

import './ngbBibliographyPanel.scss';

import component from './ngbBibliographyPanel.component';
import controller from './ngbBibliographyPanel.controller';
import service from './ngbBibliographyPanel.service';

export default angular
    .module('ngbBibliographyPanel', [])
    .controller(controller.UID, controller)
    .component('ngbBibliographyPanel', component)
    .service('ngbBibliographyPanelService', service.instance)
    .name;
