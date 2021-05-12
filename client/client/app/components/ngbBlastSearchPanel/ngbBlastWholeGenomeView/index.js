import angular from 'angular';

// Import Style
import './ngbBlastWholeGenomeView.scss';

// Import internal modules
import controller from './ngbBlastWholeGenomeView.controller';
import component from './ngbBlastWholeGenomeView.component';
import run from './ngbBlastWholeGenomeView.run';
// Import external modules

export default angular
    .module('ngbBlastWholeGenomeView', [])
    .controller(controller.UID, controller)
    .component('ngbBlastWholeGenomeView', component)
    .run(run)
    .name;
