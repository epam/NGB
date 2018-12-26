import angular from 'angular';

import controller from './ngbMainSettingsDlg.controller';
import controllerRow from './ngbCustomizationTableRow/ngbCustomizationTableRow.controller';
import ngbMainSettingsDlgService from './ngbMainSettingsDlg.service';
import ngbMainSettingsDlgConstant from './ngbMainSettingsDlg.constant';
import customizationTableRowDirective from './ngbCustomizationTableRow/ngbCustomizationTableRow.directive';

import moment from '../../../../compat/moment';

export default angular.module('ngbMainSettingsDlgComponent', [moment])
    .service('ngbMainSettingsDlgService', ngbMainSettingsDlgService)
    .controller(controller.UID, controller)
    .controller(controllerRow.UID, controllerRow)
    .directive('ngbCustomizationTableRow', customizationTableRowDirective)
    .constant('ngbMainSettingsDlgConstant', ngbMainSettingsDlgConstant)
    .name;
