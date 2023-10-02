import './ngbDiseasesTableContextMenu.scss';

import angular from 'angular';

import ngbContextMenuBuilder from '../../../../../../shared/ngbContextMenu';
import controller from './ngbDiseasesTableContextMenu.controller';
import factory from './ngbDiseasesTableContextMenu.factory';

export default angular
    .module('ngbDiseasesTableContextMenu', [ngbContextMenuBuilder])
    .factory('ngbDiseasesTableContextMenu', factory)
    .controller(controller.UID, controller)
    .name;
