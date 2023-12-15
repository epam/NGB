import angular from 'angular';

import './ngbTargetsTable.scss';
// import './ngbTargetLaunchDialog/ngbTargetLaunchDialog.scss';
import './ngbTargetLaunchDialog2/ngbTargetLaunchDialog2.scss';
import './ngbTargetSavedIdentificationsDialog/ngbTargetSavedIdentificationsDialog.scss';

import component from './ngbTargetsTable.component';
import controller from './ngbTargetsTable.controller';
import service from './ngbTargetsTable.service';
// import runLaunch from './ngbTargetLaunchDialog/ngbTargetLaunchDialog.run';
import runLaunch from './ngbTargetLaunchDialog2/ngbTargetLaunchDialog2.run';
import runSaved from './ngbTargetSavedIdentificationsDialog/ngbTargetSavedIdentificationsDialog.run';

import ngbTargetsTableActions from './ngbTargetsTableActions';
import ngbTargetsTableFilter from './ngbTargetsTableFilter';
import ngbTargetsTablePaginate from './ngbTargetsTablePaginate';
import ngbDisableWheelHandler from './disable-wheel-handler';
import ngbMdChips from './md-chips-trim';

import ngbLaunchMenuInput from './ngbTargetLaunchDialog2/ngbLaunchMenuInput.directive';

export default angular
    .module('ngbTargetsTable', [
        ngbTargetsTableActions,
        ngbTargetsTableFilter,
        ngbTargetsTablePaginate,
        ngbLaunchMenuInput,
    ])
    .directive('disableWheelHandler', ngbDisableWheelHandler)
    .directive('mdChipsTrim', ngbMdChips)
    .controller(controller.UID, controller)
    .component('ngbTargetsTable', component)
    .service('ngbTargetsTableService', service.instance)
    .run(runLaunch)
    .run(runSaved)
    .name;
