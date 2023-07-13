import angular from 'angular';

import './ngbIdentificationsTab.scss';

import component from './ngbIdentificationsTab.component';
import controller from './ngbIdentificationsTab.controller';
import service from './ngbIdentificationsTab.service';

import ngbKnownDrugsPanel from './ngbKnownDrugsPanel';
import ngbBibliographyPanel from './ngbBibliographyPanel';

export default angular
    .module('ngbIdentificationsTab', [ngbKnownDrugsPanel, ngbBibliographyPanel])
    .controller(controller.UID, controller)
    .component('ngbIdentificationsTab', component)
    .service('ngbIdentificationsTabService', service.instance)
    .name;
