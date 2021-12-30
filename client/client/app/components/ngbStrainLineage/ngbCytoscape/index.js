import angular from 'angular';

// Import Style
import 'cytoscape-context-menus/cytoscape-context-menus.css';

// Import internal modules
import cytoscapeComponent from './ngbCytoscape.component';
import cytoscapeController from './ngbCytoscape.controller';
import './ngbCytoscape.scss';

import cytoscapeSettings from './ngbCytoscape.settings';
import ngbCytoscapeToolbarPanelModule from './ngbCytoscapeToolbarPanel';

export default angular.module('ngbCytoscape', [ngbCytoscapeToolbarPanelModule])
    .constant('cytoscapeSettings', cytoscapeSettings)
    .controller(cytoscapeController.UID, cytoscapeController)
    .component('ngbCytoscape', cytoscapeComponent)
    .name;
