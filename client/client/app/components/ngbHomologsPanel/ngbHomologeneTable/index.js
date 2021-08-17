// Import Style
import './ngbHomologeneTable.scss';

import angular from 'angular';
import component from './ngbHomologeneTable.component';
import controller from './ngbHomologeneTable.controller';
import service from './ngbHomologeneTable.service';

export default angular
    .module('ngbHomologeneTable', [])
    .service('ngbHomologeneTableService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbHomologeneTable', component)
    .name;
