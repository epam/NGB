// Import Style
import './ngbFilterPanel.scss';

import angular from 'angular';
import ngMessages from 'angular-messages';

// Import internal modules
import controller from './ngbFilterPanel.controller';
import component from './ngbFilterPanel.component';

import baseFilterController from './baseFilterController';

import activeVcfFiles from './activeVcfFiles/activeVcfFiles.component';
import activeVcfFilesController from './activeVcfFiles/activeVcfFiles.controller';

import advancedVcfFilter from './advancedVcfFilter/advancedVcfFilter.component';
import advancedVcfFilterController from './advancedVcfFilter/advancedVcfFilter.controller';

import gene from './gene/gene.component';
import geneController from './gene/gene.controller';

import quality from './quality/quality.component';
import qualityController from './quality/quality.controller';

import variants from './variants/variants.component';
import variantsController from './variants/variants.controller';

import exons from './exons/exonsFilter.component';
import exonsController from './exons/exonsFilter.controller';

import ngbFilterService from './ngbFilter.service';

// Import app modules
import dataServices from '../../../dataServices/angular-module';

export default angular.module('ngbFilterPanel', [
    dataServices,
    ngMessages
])
    .controller(controller.UID, controller)
    .controller('baseFilterController', baseFilterController)
    .controller(activeVcfFilesController.UID, activeVcfFilesController)
    .controller(advancedVcfFilterController.UID, advancedVcfFilterController)
    .controller(qualityController.UID, qualityController)
    .controller(geneController.UID, geneController)
    .controller(variantsController.UID, variantsController)
    .controller(exonsController.UID, exonsController)

    .service('ngbFilterService', ngbFilterService)

    .component('ngbFilterPanel', component)
    .component('activeVcfFiles', activeVcfFiles)
    .component('advancedVcfFilter', advancedVcfFilter)
    .component('gene', gene)
    .component('quality', quality)
    .component('variants', variants)
    .component('exons', exons)
    .name;
