import angular from 'angular';

// Import Style
import './ngbFeatureInfoPanel.scss';

// Import internal modules
import controller from './ngbFeatureInfoPanel.controller';
import component from './ngbFeatureInfoPanel.component';
import run from './ngbFeatureInfoPanel.run';
import ngbFeatureInfoPanelService from './ngbFeatureInfoPanel.service';

// Import app modules
import dataServices from '../../../dataServices/angular-module';
import ngbFeatureInfo from './ngbFeatureInfo';

export default angular.module('ngbFeatureInfoPanel', [ dataServices, ngbFeatureInfo ])
    .service('ngbFeatureInfoPanelService', ngbFeatureInfoPanelService.instance)
    .component('ngbFeatureInfoPanel', component)
    .controller(controller.UID, controller)
    .run(run)
    .name;
