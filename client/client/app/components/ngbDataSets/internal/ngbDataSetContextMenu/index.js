import './styles.scss';
// Import internal modules
import angular from 'angular';
import ngbContextMenuBuilder from './ngbContextMenu';
import ngbDataSetContextMenuFactory from './ngbDataSetContextMenu.factory';
import ngbDataSetContextMenuController from './ngbDataSetContextMenu.controller';

export default angular.module('ngbDataSetContextMenu', [ngbContextMenuBuilder])
    .factory('ngbDataSetContextMenu', ngbDataSetContextMenuFactory)
    .controller('ngbDataSetContextMenuController', ngbDataSetContextMenuController)
    .name;
