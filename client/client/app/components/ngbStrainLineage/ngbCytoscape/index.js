import angular from 'angular';

// Import internal modules
import cytoscapeComponent from './ngbCytoscape.component';
import cytoscapeController from './ngbCytoscape.controller';

// Import Style
import './ngbCytoscape.scss';
import cytoscapeSettings from './ngbCytoscape.settings';

export default angular.module('ngbCytoscape', [])
    .constant('cytoscapeSettings', cytoscapeSettings)
    .controller(cytoscapeController.UID, cytoscapeController)
    .component('ngbCytoscape', cytoscapeComponent)
    .name;
