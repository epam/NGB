import angular from 'angular';

import './ngbSequencesTable.scss';

import component from './ngbSequencesTable.component';
import controller from './ngbSequencesTable.controller';
import service from './ngbSequencesTable.service';

import ngbSequencesTablePagination from './ngbSequencesTablePagination';
import ngbSequenceTableContextMenu from './ngbSequenceTableContextMenu';

export default angular
    .module('ngbSequencesTable', [ngbSequencesTablePagination, ngbSequenceTableContextMenu])
    .controller(controller.UID, controller)
    .component('ngbSequencesTable', component)
    .service('ngbSequencesTableService', service.instance)
    .name;
