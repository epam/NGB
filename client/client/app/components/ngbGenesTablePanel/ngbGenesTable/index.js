// Import Style
import './ngbGenesTable.scss';

import angular from 'angular';
import component from './ngbGenesTable.component';
import controller from './ngbGenesTable.controller';
import ngbGenesTableColumn from './ngbGenesTableColumn';
import ngbGenesTableFilter from './ngbGenesTableFilter';
import ngbGenesTableDownload from './ngbGenesTableDownload';
import service from './ngbGenesTable.service';

export default angular
    .module('ngbGenesTable', [ngbGenesTableColumn, ngbGenesTableFilter, ngbGenesTableDownload])
    .service('ngbGenesTableService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbGenesTable', component)
    .name;
