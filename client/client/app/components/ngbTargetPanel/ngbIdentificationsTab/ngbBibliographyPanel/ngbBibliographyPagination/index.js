import angular from 'angular';

import './ngbBibliographyPagination.scss';

import component from './ngbBibliographyPagination.component';
import controller from './ngbBibliographyPagination.controller';

export default angular
    .module('ngbBibliographyPagination', [])
    .controller(controller.UID, controller)
    .component('ngbBibliographyPagination', component)
    .name;
