import './styles.scss';
// Import internal modules
import angular from 'angular';
import ngbContextMenu from './ngbContextMenu';
import ngbDataSetContextMenuComponent from './component';
import ngbDataSetContextMenuController from './controller';

export default angular.module('ngbDataSetContextMenu', [ngbContextMenu])
    .factory('ngbDataSetContextMenu', ngbDataSetContextMenuComponent)
    .controller('ngbDataSetContextMenuController', ngbDataSetContextMenuController)
    .name;
