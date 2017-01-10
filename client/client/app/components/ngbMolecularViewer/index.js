import angular from 'angular';

// Import Style
import './ngbMolecularViewer.scss';

// Import internal modules
import controller from './ngbMolecularViewer.controller';
import constant from './ngbMolecularViewer.constant';
import component from './ngbMolecularViewer.component';
import service from './ngbMolecularViewer.service';

import miewComponent from './ngbMiew';
import pdbDescriptionComponent from './ngbPdbDescription';

export default angular.module('ngbMolecularViewer', [miewComponent, pdbDescriptionComponent])
    .component('ngbMolecularViewer', component)
    .controller(controller.UID, controller)
    .constant('ngbMolecularViewerConstant', constant)
    .service('ngbMolecularViewerService', service.instance)
    .name;
