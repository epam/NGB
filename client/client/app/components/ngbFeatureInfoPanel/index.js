import angular from 'angular';

// Import Style
import './ngbFeatureInfoPanel.scss';

// Import internal modules
import controller from './ngbFeatureInfoPanel.controller';
import component from './ngbFeatureInfoPanel.component';
import run from './ngbFeatureInfoPanel.run';

// Import app modules
import dataServices from '../../../dataServices/angular-module';
import ngbFeatureInfo from './ngbFeatureInfo';

export default angular.module('ngbFeatureInfoPanel', [ dataServices, ngbFeatureInfo ])
    .component('ngbFeatureInfoPanel', component)
    .controller(controller.UID, controller)
    .run(run)
    .name;
