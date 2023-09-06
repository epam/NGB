import angular from 'angular';

import './ngbGenomicsTable.scss';

import component from './ngbGenomicsTable.component';
import controller from './ngbGenomicsTable.controller';

import ngbGenomicsTablePagination from './ngbGenomicsTablePagination';
import ngbGenomicsTableFilter from './ngbGenomicsTableFilter';
import ngbHomologsDomains from '../../../../ngbHomologsPanel/ngbHomologsDomains';

export default angular
    .module('ngbGenomicsTable', [ngbGenomicsTablePagination, ngbGenomicsTableFilter, ngbHomologsDomains])
    .controller(controller.UID, controller)
    .component('ngbGenomicsTable', component)
    .name;
