import angular from 'angular';

import './ngbGenomicsTableFilter.scss';

import component from './ngbGenomicsTableFilter.component';
import controller from './ngbGenomicsTableFilter.controller';

export default angular.module('ngbGenomicsTableFilter', [])
    .controller(controller.UID, controller)
    .component('ngbGenomicsTableFilter', component)
    .name;
