import angular from 'angular';

// Import internal modules
import cytoscapeComponent from './ngbCytoscapePathway.component';
import cytoscapeController from './ngbCytoscapePathway.controller';

// Import Style
import './ngbCytoscapePathway.scss';
import cytoscapeSettings from './ngbCytoscapePathway.settings';

export default angular.module('ngbCytoscapePathway', [])
    .constant('cytoscapeSettings', cytoscapeSettings)
    .controller(cytoscapeController.UID, cytoscapeController)
    .component('ngbCytoscapePathway', cytoscapeComponent)
    .name;
