import angular from 'angular';
import dataServices from '../../../../../../../dataServices/angular-module';
import uiGrid from '../../../../../../compat/uiGrid';

import controller from './ngbUserForm.controller';

import './ngbUserForm.scss';

import ngbUserFormService from './ngbUserForm.service';
import ngbUserManagementGridOptionsConstant from '../ngbUserManagementGridOptions.constant';

export default angular.module('ngbUserFormComponent', [dataServices, uiGrid])
    .service('ngbUserFormService', ngbUserFormService)
    .controller(controller.UID, controller)
    .constant('ngbUserManagementGridOptionsConstant', ngbUserManagementGridOptionsConstant)
    .name;
