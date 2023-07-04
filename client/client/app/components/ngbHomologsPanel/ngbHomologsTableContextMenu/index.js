import angular from 'angular';

import './ngbHomologsTableContextMenu.scss';

import ngbContextMenuBuilder from '../../../shared/ngbContextMenu';
import controller from './ngbHomologsTableContextMenu.controller';
import factory from './ngbHomologsTableContextMenu.factory';

export default angular.module('ngbHomologsTableContextMenu', [ngbContextMenuBuilder])
    .factory('ngbHomologsTableContextMenu', factory)
    .controller('ngbHomologsTableContextMenuController', controller)
    .name;
