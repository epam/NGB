import angular from 'angular';

// Import internal modules --> components
import ngbMainSettingsDlgComponent from './ngbUserManagementDlg';

import userManagementBtnComponent from './ngbUserManagement.component';
import controller from './ngbUserManagement.controller';
import ngbUser from './ngbUser';


export default angular.module('ngbUserManagementComponent', [
    ngbMainSettingsDlgComponent,
    ngbUser
])
    .component('ngbUserManagement', userManagementBtnComponent)
    .controller(controller.UID, controller)
    .name;
