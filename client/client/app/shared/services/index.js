import angular from 'angular';
import apiService from './api.service';
import colorService from './color.service';
import stateParamsService from './stateParams.service';
import trackNamingService from './trackNaming.service';

export default angular.module('ngbAppServices', [])
    .service('stateParamsService', stateParamsService.instance)
    .service('colorService', colorService.instance)
    .service('apiService', apiService.instance)
    .service('trackNamingService', trackNamingService.instance)
    .name;
