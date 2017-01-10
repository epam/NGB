import angular from 'angular';

// Import Style
import './ngbFeatureInfoPanel.scss';

// Import internal modules
import controller from './ngbFeatureInfoPanel.controller';
import component from './ngbFeatureInfoPanel.component';
import run from './ngbFeatureInfoPanel.run';


export default angular.module('ngbFeatureInfoPanel', [])
    .component('ngbFeatureInfoPanel', component)
    .controller(controller.UID, controller)
    .run(run)
    .name;
