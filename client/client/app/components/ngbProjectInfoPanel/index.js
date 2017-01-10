// Import Style
import './ngbProjectInfoPanel.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbProjectInfoPanel.controller';
import component from './ngbProjectInfoPanel.component';
import run from './ngbProjectInfoPanel.run';

// Import external modules

// Import app modules
import dataServices from '../../../dataServices/angular-module';
import ngbProjectInfo from './ngbProjectInfo';

export default angular.module('ngbProjectInfoPanel', [dataServices, ngbProjectInfo])
    .controller(controller.UID, controller)
    .component('ngbProjectInfoPanel', component)
    .run(run)
    .name;