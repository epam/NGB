import './ngbDiseaseContextMenu.scss';

import angular from 'angular';

import ngbContextMenuBuilder from '../../../shared/ngbContextMenu';
import controller from './ngbDiseaseContextMenu.controller';
import factory from './ngbDiseaseContextMenu.factory';

export default angular
    .module('ngbDiseaseContextMenu', [ngbContextMenuBuilder])
    .factory('ngbDiseaseContextMenu', factory)
    .controller(controller.UID, controller)
    .name;
