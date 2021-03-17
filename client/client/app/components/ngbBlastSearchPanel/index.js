import angular from 'angular';

// Import Style
import './ngbBlastSearchPanel.scss';

// Import internal modules
import ngbBlastSearchPanel from './ngbBlastSearchPanel.component';
import controller from './ngbBlastSearchPanel.controller';

// Import components
import ngbBlastWholeGenomeView from './ngbBlastWholeGenomeView';

// Import external modules
export default angular
    .module('ngbBlastSearchPanel', [ngbBlastWholeGenomeView])
    .component('ngbBlastSearchPanel', ngbBlastSearchPanel)
    .controller(controller.UID, controller).name;