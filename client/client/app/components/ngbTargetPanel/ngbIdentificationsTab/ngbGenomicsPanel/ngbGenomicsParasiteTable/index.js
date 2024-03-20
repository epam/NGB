import angular from 'angular';

import './ngbGenomicsParasiteTable.scss';

import component from './ngbGenomicsParasiteTable.component';
import controller from './ngbGenomicsParasiteTable.controller';

import ngbGenomicsTableFilter from '../ngbGenomicsTable/ngbGenomicsTableFilter';
import ngbGenomicsTablePagination from '../ngbGenomicsTable/ngbGenomicsTablePagination';

export default angular
    .module('ngbGenomicsParasiteTable', [ngbGenomicsTableFilter, ngbGenomicsTablePagination])
    .controller(controller.UID, controller)
    .component('ngbGenomicsParasiteTable', component)
    .name;
