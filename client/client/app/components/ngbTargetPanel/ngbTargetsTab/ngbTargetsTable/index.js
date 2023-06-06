import angular from 'angular';

import './ngbTargetsTable.scss';
import './ngbTargetLaunchDialog/ngbTargetLaunchDialog.scss';

import component from './ngbTargetsTable.component';
import controller from './ngbTargetsTable.controller';
import service from './ngbTargetsTable.service';
import run from './ngbTargetLaunchDialog/ngbTargetLaunchDialog.run';

import ngbTargetsTableActions from './ngbTargetsTableActions';
import ngbTargetsTableFilter from './ngbTargetsTableFilter';
import ngbTargetsTablePaginate from './ngbTargetsTablePaginate';
import ngbDisableWheelHandler from './disable-wheel-handler';

export default angular
    .module('ngbTargetsTable', [ngbTargetsTableActions, ngbTargetsTableFilter, ngbTargetsTablePaginate])
    .directive('disableWheelHandler', ngbDisableWheelHandler)
    .controller(controller.UID, controller)
    .component('ngbTargetsTable', component)
    .service('ngbTargetsTableService', service.instance)
    .run(run)
    .name;
