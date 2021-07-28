// Import internal modules
import angular from 'angular';
import ngbContextMenuBuilder from './ngbContextMenu.builder';
import ngbContextMenuDirective from './ngbContextMenu.directive';

export default angular.module('ngbContextMenu', [])
    .factory('ngbContextMenuBuilder', [
        '$q',
        '$compile',
        '$animate',
        '$rootScope',
        '$controller',
        ngbContextMenuBuilder
    ])
    .directive('ngbContextMenu', ngbContextMenuDirective)
    .name;
