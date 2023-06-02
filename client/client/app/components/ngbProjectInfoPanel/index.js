// Import Style
import './ngbProjectInfoPanel.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbProjectInfoPanel.controller';
import component from './ngbProjectInfoPanel.component';
import ngbProjectInfoSections from './ngbProjectInfoSections';

// Import external modules

// Import app modules
import dataServices from '../../../dataServices/angular-module';
import ngbProjectInfo from './ngbProjectInfo';
import ngbProjectInfoEditNote from './ngbProjectInfoEditNote';
import service from './ngbProjectInfo.service';

export default angular
    .module('ngbProjectInfoPanel', [
        dataServices, ngbProjectInfo,
        ngbProjectInfoSections, ngbProjectInfoEditNote
    ])
    .controller(controller.UID, controller)
    .component('ngbProjectInfoPanel', component)
    .service('ngbProjectInfoService', service)
    .name;
