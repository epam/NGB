import angular from 'angular';
import colorService from './color.service';
import stateParamsService from './stateParams.service';
import apiService from './api.service';


export default angular.module('ngbAppServices', [])
    .service('stateParamsService', stateParamsService.instance)
    .service('colorService', colorService.instance)
    .service('apiService', apiService.instance)
    .name;
