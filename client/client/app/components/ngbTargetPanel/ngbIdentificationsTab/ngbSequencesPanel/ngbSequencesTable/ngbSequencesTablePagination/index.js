import angular from 'angular';

import './ngbSequencesTablePagination.scss';

import component from './ngbSequencesTablePagination.component';
import controller from './ngbSequencesTablePagination.controller';

export default angular
    .module('ngbSequencesTablePagination', [])
    .controller(controller.UID, controller)
    .component('ngbSequencesTablePagination', component)
    .name;
