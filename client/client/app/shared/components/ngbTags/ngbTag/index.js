import angular from 'angular';
import '../ngbTags.scss';
import controller from './ngbTag.controller';
import component from './ngbTag.component';


export default angular.module('ngbTag', [])
    .controller(controller.UID, controller)
    .component('ngbTag', component)
    .name;
