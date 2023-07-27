import angular from 'angular';
import './ngbTags.scss';
import controller from './ngbTags.controller';
import component from './ngbTags.component';
import ngbTag from './ngbTag';

export default angular.module('ngbTags', [ngbTag])
    .controller(controller.UID, controller)
    .component('ngbTags', component)
    .name;
