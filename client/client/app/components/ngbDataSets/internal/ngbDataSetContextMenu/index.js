import './styles.scss';
// Import internal modules
import angular from 'angular';
import ngbContextMenuBuilder from '../../../../shared/ngbContextMenu';
import ngbDataSetContextMenuController from './ngbDataSetContextMenu.controller';
import ngbDataSetContextMenuFactory from './ngbDataSetContextMenu.factory';

export default angular.module('ngbDataSetContextMenu', [ngbContextMenuBuilder])
    .factory('ngbDataSetContextMenu', ngbDataSetContextMenuFactory)
    .controller('ngbDataSetContextMenuController', ngbDataSetContextMenuController)
    .name;
