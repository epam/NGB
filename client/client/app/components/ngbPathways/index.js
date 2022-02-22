import angular from 'angular';
import cytoscapePathwayComponent from './ngbCytoscapePathway';

// Import components
import ngbInternalPathwaysResult from './ngbInternalPathwaysResult';
import ngbInternalPathwaysTable from './ngbInternalPathwaysTable';
import service from './ngbPathways.service';
import ngbPathwaysAnnotation from './ngbPathwaysAnnotation';
import ngbPathwaysColorSchemePreference from './ngbPathwaysColorSchemePreference';

// Import internal modules
import ngbPathwaysPanel from './ngbPathwaysPanel.component';
import controller from './ngbPathwaysPanel.controller';

// Import Style
import './ngbPathwaysPanel.scss';
import ngbPathwaysPanelPaginate from './ngbPathwaysPanelPaginate';

// Import external modules
export default angular
    .module('ngbPathwaysPanel', [
        cytoscapePathwayComponent,
        ngbInternalPathwaysTable,
        ngbInternalPathwaysResult,
        ngbPathwaysPanelPaginate,
        ngbPathwaysColorSchemePreference,
        ngbPathwaysAnnotation
    ])
    .service('ngbPathwaysService', service.instance)
    .component('ngbPathwaysPanel', ngbPathwaysPanel)
    .controller(controller.UID, controller)
    .name;
