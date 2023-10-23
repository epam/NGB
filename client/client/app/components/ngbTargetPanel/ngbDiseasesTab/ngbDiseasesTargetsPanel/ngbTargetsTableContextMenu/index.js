import angular from 'angular';

import './ngbTargetsTableContextMenu.scss';

import ngbContextMenuBuilder from '../../../../../shared/ngbContextMenu';
import controller from './ngbTargetsTableContextMenu.controller';
import factory from './ngbTargetsTableContextMenu.factory';

export default angular
    .module('ngbTargetsTableContextMenu', [ngbContextMenuBuilder])
    .factory('ngbTargetsTableContextMenu', factory)
    .controller(controller.UID, controller)
    .name;
