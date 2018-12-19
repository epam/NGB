// Import internal modules
import angular from 'angular';
import ngbDataSetContextMenuDirective from './directive';
import ngbDataSetContextMenuFactory from './factory';

export default angular.module('ngbContextMenu', [])
    .factory('ngbContextMenu', [
        '$q',
        '$http',
        '$timeout',
        '$compile',
        '$animate',
        '$rootScope',
        '$controller',
        ngbDataSetContextMenuFactory
    ])
    .directive('hasContextMenu', [
        '$injector',
        '$window',
        '$parse',
        '$timeout',
        'userDataService',
        ngbDataSetContextMenuDirective
    ])
    .name;
