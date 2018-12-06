import angular from 'angular';
import dataServices from '../../../../../../dataServices/angular-module';
import uiGrid from '../../../../../compat/uiGrid';

import './ngbUserManagement.scss';
import ngbUserManagementGridOptionsConstant from './ngbUserManagementGridOptions.constant';

import ngbUserFormComponent from './ngbUserForm';
import ngbRoleFormComponent from './ngbRoleForm';

import ngbUserManagement from './ngbUserManagement.component';
import ngbUserManagementService from './ngbUserManagement.service';

import controller from './ngbUserManagement.controller';

export default angular.module('ngbUserManagementComponent', [dataServices, ngbUserFormComponent, ngbRoleFormComponent, uiGrid])
    .service('ngbUserManagementService', ngbUserManagementService)
    .component('ngbUserManagement', ngbUserManagement)
    .controller(controller.UID, controller)
    .constant('ngbUserManagementGridOptionsConstant', ngbUserManagementGridOptionsConstant)
    .name;
