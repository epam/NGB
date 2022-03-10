// Import Style
import angular from 'angular';

// Import components
import ngbInternalPathwayNode from '../ngbInternalPathwayNode';

// Import internal modules
import cytoscapeComponent from './ngbCytoscapePathway.component';
import cytoscapeController from './ngbCytoscapePathway.controller';
import './ngbCytoscapePathway.scss';
import cytoscapeSettings from './ngbCytoscapePathway.settings';

export default angular.module('ngbCytoscapePathway', [ngbInternalPathwayNode])
    .constant('cytoscapePathwaySettings', cytoscapeSettings)
    .controller(cytoscapeController.UID, cytoscapeController)
    .component('ngbCytoscapePathway', cytoscapeComponent)
    .name;
