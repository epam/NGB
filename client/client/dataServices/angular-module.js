import angular from 'angular';
import * as dataServices from './index';
import LocalDataService from './local/local-data-service';

export default angular.module('ngbDataServices', [])
    .service('bamDataService', dataServices.BamDataService.serviceFactory)
    .service('bedDataService', dataServices.BedDataService.serviceFactory)
    .service('bookmarkDataService', dataServices.BookmarkDataService.serviceFactory)
    .service('geneDataService', dataServices.GeneDataService.serviceFactory)
    .service('genomeDataService', dataServices.GenomeDataService.serviceFactory)
    .service('localDataService', ($window) => new LocalDataService($window))
    .service('projectDataService', dataServices.ProjectDataService.serviceFactory)
    .service('segDataService', dataServices.SegDataService.serviceFactory)
    .service('othersDataService', dataServices.OthersDataService.serviceFactory)
    .service('vcfDataService', dataServices.VcfDataService.serviceFactory)
    .service('wigDataService', dataServices.WigDataService.serviceFactory)
    .service('mafDataService', dataServices.MafDataService.serviceFactory)
    .service('bucketDataService', dataServices.BucketDataService.serviceFactory)
    .service('externaldbDataService', dataServices.ExternaldbDataService.serviceFactory)
    .name;
