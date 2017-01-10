import angular from 'angular';
import colorService from './color.service';
import stateParamsService from './stateParams.service';


export default angular.module('ngbAppServices', [])
    .service('stateParamsService', stateParamsService.instance)
    .service('colorService', colorService.instance)
    .name;
