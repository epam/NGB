import angular from 'angular';

import './ngbGenomicsAlignment.scss';

import component from './ngbGenomicsAlignment.component';
import controller from './ngbGenomicsAlignment.controller';

export default angular
    .module('ngbGenomicsAlignment', [])
    .controller(controller.UID, controller)
    .component('ngbGenomicsAlignment', component)
    .name;
