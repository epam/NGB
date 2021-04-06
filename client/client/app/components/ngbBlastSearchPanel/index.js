import angular from 'angular';

// Import Style
import './ngbBlastSearchPanel.scss';

// Import internal modules
import ngbBlastSearchPanel from './ngbBlastSearchPanel.component';
import controller from './ngbBlastSearchPanel.controller';
import service from './ngbBlastSearch.service';

// Import components
import ngbBlastWholeGenomeView from './ngbBlastWholeGenomeView';

// Import external modules
export default angular
    .module('ngbBlastSearchPanel', [ngbBlastWholeGenomeView])
    .service('ngbBlastSearchService', service.instance)
    .component('ngbBlastSearchPanel', ngbBlastSearchPanel)
    .controller(controller.UID, controller)
    .name;
