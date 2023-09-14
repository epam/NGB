import './styles.scss';

import angular from 'angular';

import ngbContextMenuBuilder from '../../../../../../shared/ngbContextMenu';
import controller from './ngbSequenceTableContextMenu.controller';
import factory from './ngbSequenceTableContextMenu.factory';

export default angular
    .module('ngbSequenceTableContextMenu', [ngbContextMenuBuilder])
    .factory('ngbSequenceTableContextMenu', factory)
    .controller(controller.UID, controller)
    .name;
