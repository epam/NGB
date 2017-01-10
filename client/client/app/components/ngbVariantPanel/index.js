// Import Style
import './ngbVariantPanel.scss';

import angular from 'angular';

// Import internal modules
import controller from './ngbVariantPanel.controller';
import component from './ngbVariantPanel.component';
import run from './ngbVariantPanel.run';
// Import external modules


export default angular.module('ngbVariantPanel', [])
    .controller(controller.UID, controller)
    .component('ngbVariantPanel', component)
    .run(run)
    .name;
