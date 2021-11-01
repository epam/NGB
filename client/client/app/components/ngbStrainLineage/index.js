import angular from 'angular';
import cytoscapeComponent from './ngbCytoscape';

import component from './ngbStrainLineage.component';

// Import internal modules
import controller from './ngbStrainLineage.controller';

// Import Style
import './ngbStrainLineage.scss';
import service from './ngbStrainLineage.service';
import ngbStrainLineageNode from './ngbStrainLineageNode';

export default angular.module('ngbStrainLineage', [cytoscapeComponent, ngbStrainLineageNode])
    .component('ngbStrainLineage', component)
    .controller(controller.UID, controller)
    .service('ngbStrainLineageService', service.instance)
    .name;
