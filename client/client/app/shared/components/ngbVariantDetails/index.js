// Import Style
import './ngbVariantDetails.scss';
import './ngbVariantInfo/ngbVariantInfo.scss';
import './ngbVariantAnnotations/ngbVariantAnnotations.scss';
import './ngbVariantVisualizer/ngbVariantVisualizer.scss';
import './ngbVariantDbSnp/ngbVariantDbSnp.scss';


import angular from 'angular';

// Import internal modules

import ngbVariantInfoController from './ngbVariantInfo/ngbVariantInfo.controller';
import ngbVariantInfoComponent from './ngbVariantInfo/ngbVariantInfo.component';
import ngbVariantInfoService from './ngbVariantInfo/ngbVariantInfo.service';

import ngbVariantAnnotationsController from './ngbVariantAnnotations/ngbVariantAnnotations.controller';
import ngbVariantAnnotationsComponent from './ngbVariantAnnotations/ngbVariantAnnotations.component';

import ngbVariantVisualizerController from './ngbVariantVisualizer/ngbVariantVisualizer.controller';
import ngbVariantVisualizerComponent from './ngbVariantVisualizer/ngbVariantVisualizer.component';
import ngbVariantVisualizerService from './ngbVariantVisualizer/ngbVariantVisualizer.service';

import ngbVariantDbSnpController from './ngbVariantDbSnp/ngbVariantDbSnp.controller';
import ngbVariantDbSnpComponent from './ngbVariantDbSnp/ngbVariantDbSnp.component';
import ngbVariantDbSnpService from './ngbVariantDbSnp/ngbVariantDbSnp.service';

import ngbVariantDetailsConstants from './ngbVariantDetails.constant.js';
import dataServices from '../../../../dataServices/angular-module';
import {capitalizeFilter} from './ngbVariantDetails.controller';


export default angular.module('ngbVariantDetails' , [dataServices])
    .constant('constants', ngbVariantDetailsConstants)
    .service('ngbVariantInfoService', ngbVariantInfoService.instance)
    .service('ngbVariantVisualizerService', ngbVariantVisualizerService.instance)
    .service('ngbVariantDbSnpService', ngbVariantDbSnpService.instance)
    .controller(ngbVariantInfoController.UID, ngbVariantInfoController)
    .controller(ngbVariantAnnotationsController.UID, ngbVariantAnnotationsController)
    .controller(ngbVariantVisualizerController.UID, ngbVariantVisualizerController)
    .controller(ngbVariantDbSnpController.UID, ngbVariantDbSnpController)
    .component('ngbVariantInfo', ngbVariantInfoComponent)
    .component('ngbVariantAnnotations', ngbVariantAnnotationsComponent)
    .component('ngbVariantVisualizer', ngbVariantVisualizerComponent)
    .component('ngbVariantDbSnp', ngbVariantDbSnpComponent)
    .filter('capitalize', capitalizeFilter)
    .name;
