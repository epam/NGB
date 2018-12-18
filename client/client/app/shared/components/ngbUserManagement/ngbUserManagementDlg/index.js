import angular from 'angular';
import dataServices from '../../../../../dataServices/angular-module';
import uiGrid from '../../../../compat/uiGrid';

import './ngbUserManagementDlg.scss';
import ngbUserManagementGridOptionsConstant from './ngbUserManagementGridOptions.constant';

import ngbUserFormComponent from './ngbUserForm';
import ngbRoleFormComponent from './ngbRoleForm';

import ngbUserManagement from './ngbUserManagementDlg.component';
import ngbUserManagementDlgService from './ngbUserManagementDlg.service';

import controller from './ngbUserManagementDlg.controller';

export default angular.module('ngbUserManagementDlgComponent', [dataServices, ngbUserFormComponent, ngbRoleFormComponent, uiGrid])
    .service('ngbUserManagementService', ngbUserManagementDlgService)
    .component('ngbUserManagementDlg', ngbUserManagement)
    .controller(controller.UID, controller)
    .constant('ngbUserManagementGridOptionsConstant', ngbUserManagementGridOptionsConstant)
    .name;
