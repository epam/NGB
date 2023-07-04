// Import Style
import './ngbOrthoParaTable.scss';

import angular from 'angular';
import component from './ngbOrthoParaTable.component';
import controller from './ngbOrthoParaTable.controller';
import service from './ngbOrthoParaTable.service';

import ngbHomologsTableContextMenu from '../ngbHomologsTableContextMenu';

export default angular
    .module('ngbOrthoParaTable', [ngbHomologsTableContextMenu])
    .service('ngbOrthoParaTableService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbOrthoParaTable', component)
    .name;
