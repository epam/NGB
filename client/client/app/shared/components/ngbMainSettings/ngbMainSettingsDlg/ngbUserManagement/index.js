import angular from 'angular';
import dataServices from '../../../../../../dataServices/angular-module';
import uiGrid from '../../../../../compat/uiGrid';

import './ngbUserManagement.scss';

import ngbUserManagement from './ngbUserManagement.component';
import ngbUserManagementService from './ngbUserManagement.service';

import controller from './ngbUserManagement.controller';

export default angular.module('ngbUserManagementComponent', [dataServices, uiGrid])
    .service('ngbUserManagementService', ngbUserManagementService)
    .component('ngbUserManagement', ngbUserManagement)
    .controller(controller.UID, controller)
    .name;
