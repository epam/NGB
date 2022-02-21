import angular from 'angular';

import './ngbVcfSampleAlises.scss';

import controller from './ngbVcfSampleAliases.controller';
import component from './ngbVcfSampleAliases.component';
import run from './ngbVcfSampleAliases.run';

export default angular
    .module('ngbVcfSampleAliases', [])
    .controller(controller.UID, controller)
    .component('ngbVcfSampleAliases', component)
    .run(run)
    .name;
