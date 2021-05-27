// Import Style
import './ngbBlastHistory.scss';

import angular from 'angular';
import component from './ngbBlastHistory.component';
import controller from './ngbBlastHistory.controller';
import ngbBlastSearchPanelPaginate from '../ngbBlastSearchPanelPaginate';
import service from './ngbBlastHistoryTable.service';

export default angular
    .module('ngbBlastHistory', [ngbBlastSearchPanelPaginate])
    .service('ngbBlastHistoryTableService', service.instance)
    .controller(controller.UID, controller)
    .component('ngbBlastHistory', component)
    .name;
