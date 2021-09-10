// Import Style
import './ngbDataSets.scss';
import './internal/ngbPermissionsForm/ngbPermissionsForm.scss';
import './internal/ngbDataSetMetadata/ngbDataSetMetadata.styles.scss';
// Import internal modules
import angular from 'angular';
import component from './ngbDataSets.component';
import controller from './ngbDataSets.controller';
import service from './ngbDataSets.service';
import ngbPermissionsFormService from './internal/ngbPermissionsForm/ngbPermissionsForm.service';
import indeterminateCheckbox from './internal/ngbDataSets.indeterminateCheckbox';
import ngbPermissionsGridOptionsConstant from './internal/ngbPermissionsForm/ngbItemPermissionsFormGridOptions.constant';
import ngbDataSetContextMenu from './internal/ngbDataSetContextMenu';

export default angular.module('ngbDataSets', [ngbDataSetContextMenu])
    .directive('indeterminateCheckbox', indeterminateCheckbox)
    .service('ngbPermissionsFormService', ngbPermissionsFormService)
    .service('ngbDataSetsService', service.instance)
    .controller(controller.UID, controller)
    .constant('ngbPermissionsGridOptionsConstant', ngbPermissionsGridOptionsConstant)
    .component('ngbDataSets', component)
    .name;
