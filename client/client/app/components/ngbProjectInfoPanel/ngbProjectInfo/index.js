// Import Style
import './ngbProjectInfo.scss';
import './ngbProjectSummary/ngbProjectSummary.scss';
import './ngbVariantTypeDiagram/ngbVariantTypeDiagram.scss';
import './ngbVariantQualityDiagram/ngbVariantQualityDiagram.scss';
import './ngbVariantDensityDiagram/ngbVariantDensityDiagram.scss';


import angular from 'angular';

// Import internal modules
import dataServices from '../../../../dataServices/angular-module';
import ngbProjectSummaryController from './ngbProjectSummary/ngbProjectSummary.controller';
import ngbProjectSummaryComponent from './ngbProjectSummary/ngbProjectSummary.component';

import ngbVariantTypeDiagramConstants from './ngbVariantTypeDiagram/ngbVariantTypeDiagram.constant';
import ngbVariantTypeDiagramController from './ngbVariantTypeDiagram/ngbVariantTypeDiagram.controller';
import ngbVariantTypeDiagramComponent from './ngbVariantTypeDiagram/ngbVariantTypeDiagram.component';

import ngbVariantQualityDiagramConstants from './ngbVariantQualityDiagram/ngbVariantQualityDiagram.constant';
import ngbVariantQualityDiagramController from './ngbVariantQualityDiagram/ngbVariantQualityDiagram.controller';
import ngbVariantQualityDiagramComponent from './ngbVariantQualityDiagram/ngbVariantQualityDiagram.component';

import ngbVariantDensityDiagramConstants from './ngbVariantDensityDiagram/ngbVariantDensityDiagram.constant';
import ngbVariantDensityDiagramController from './ngbVariantDensityDiagram/ngbVariantDensityDiagram.controller';
import ngbVariantDensityDiagramComponent from './ngbVariantDensityDiagram/ngbVariantDensityDiagram.component';


export default angular.module('ngbProjectInfo' , [ dataServices, 'nvd3'])
    .constant('ngbVariantTypeDiagramConstants', ngbVariantTypeDiagramConstants)
    .constant('ngbVariantQualityDiagramConstants', ngbVariantQualityDiagramConstants)
    .constant('ngbVariantDensityDiagramConstants', ngbVariantDensityDiagramConstants)
    .controller(ngbProjectSummaryController.UID, ngbProjectSummaryController)
    .controller(ngbVariantTypeDiagramController.UID, ngbVariantTypeDiagramController)
    .controller(ngbVariantQualityDiagramController.UID, ngbVariantQualityDiagramController)
    .controller(ngbVariantDensityDiagramController.UID, ngbVariantDensityDiagramController)
    .component('ngbProjectSummary', ngbProjectSummaryComponent)
    .component('ngbVariantTypeDiagram', ngbVariantTypeDiagramComponent)
    .component('ngbVariantQualityDiagram', ngbVariantQualityDiagramComponent)
    .component('ngbVariantDensityDiagram', ngbVariantDensityDiagramComponent)

    .name;
