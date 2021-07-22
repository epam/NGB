import './styles.scss';
// Import internal modules
import angular from 'angular';
import ngbContextMenuBuilder from '../../../../shared/ngbContextMenu';
import ngbGenesTableContextMenuController from './ngbGenesTableContextMenu.controller';
import ngbGenesTableContextMenuFactory from './ngbGenesTableContextMenu.factory';

export default angular.module('ngbGenesTableContextMenu', [ngbContextMenuBuilder])
    .factory('ngbGenesTableContextMenu', ngbGenesTableContextMenuFactory)
    .controller('ngbGenesTableContextMenuController', ngbGenesTableContextMenuController)
    .name;
