import './ngbVersion.scss';

import angular from 'angular';

import component from './ngbVersion.component';

export default angular.module('ngbVersionModule', [])
    .component('ngbVersion', component)
    .name;