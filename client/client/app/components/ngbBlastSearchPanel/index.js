import angular from 'angular';

// Import Style
import './ngbBlastSearchPanel.scss';

// Import internal modules
import ngbBlastSearchPanel from './ngbBlastSearchPanel.component';
import controller from './ngbBlastSearchPanel.controller';
import service from './ngbBlastSearchPanel.service';
import messages from './ngbBlastSearchPanel.messages.js';

// Import components
import ngbBlastWholeGenomeView from './ngbBlastWholeGenomeView';
import ngbBlastSearchPanelPaginate from './ngbBlastSearchPanelPaginate';

// Import external modules
export default angular
    .module('ngbBlastSearchPanel', [ngbBlastWholeGenomeView, ngbBlastSearchPanelPaginate])
    .constant('blastSearchMessages', messages)
    .service('ngbBlastSearchService', service.instance)
    .component('ngbBlastSearchPanel', ngbBlastSearchPanel)
    .controller(controller.UID, controller)
    .name;
