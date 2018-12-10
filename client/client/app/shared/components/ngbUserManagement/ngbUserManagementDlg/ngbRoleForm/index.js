import './ngbRoleForm.scss';
import angular from 'angular';
import controller from './ngbRoleForm.controller';
import dataServices from '../../../../../../dataServices/angular-module';
import ngbUserManagementGridOptionsConstant from '../ngbUserManagementGridOptions.constant';
import ngbUserRoleFormService from '../ngbUserRoleForm.service';
import uiGrid from '../../../../../compat/uiGrid';

export default angular.module('ngbRoleFormComponent', [dataServices, uiGrid])
  .service('ngbUserRoleFormService', ngbUserRoleFormService)
  .controller(controller.UID, controller)
  .constant('ngbUserManagementGridOptionsConstant', ngbUserManagementGridOptionsConstant)
  .name;
