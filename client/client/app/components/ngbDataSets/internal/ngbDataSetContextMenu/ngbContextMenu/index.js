// Import internal modules
import angular from 'angular';
import ngbDataSetContextMenuDirective from './directive';
import ngbDataSetContextMenuFactory from './factory';

export default angular.module('ngbContextMenu', [])
    .factory('ngbContextMenu', [
        '$q',
        '$compile',
        '$animate',
        '$rootScope',
        '$controller',
        ngbDataSetContextMenuFactory
    ])
    .directive('hasContextMenu', [
        '$injector',
        '$window',
        '$timeout',
        'userDataService',
        ngbDataSetContextMenuDirective
    ])
    .name;
