// Import Style
import './ngbGenesTable.scss';

import angular from 'angular';
import component from './ngbGenesTable.component';
import controller from './ngbGenesTable.controller';
import ngbGenesTableColumn from './ngbGenesTableColumn';
import ngbGenesTableFilter from './ngbGenesTableFilter';
import service from './ngbGenesTable.service';

export default angular
    .module('ngbGenesTablePanel', [ngbGenesTableColumn, ngbGenesTableFilter])
    .service('ngbGenesTableService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbGenesTablePanel', component)
    .name;
