import angular from 'angular';

import './ngbTargetContextMenu.scss';
import './ngbTargetPermissionsForm/ngbTargetPermissionsForm.scss';

import ngbContextMenuBuilder from '../../../../../shared/ngbContextMenu';
import controller from './ngbTargetContextMenu.controller';
import factory from './ngbTargetContextMenu.factory';

export default angular
    .module('ngbTargetContextMenu', [ngbContextMenuBuilder])
    .factory('ngbTargetContextMenu', factory)
    .controller(controller.UID, controller)
    .name;
