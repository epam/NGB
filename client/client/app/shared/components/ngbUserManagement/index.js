import angular from 'angular';

// Import internal modules --> components
import ngbMainSettingsDlgComponent from './ngbUserManagementDlg';

import userManagementBtnComponent from './ngbUserManagement.component';
import controller from './ngbUserManagement.controller';


export default angular.module('ngbUserManagementComponent', [
    ngbMainSettingsDlgComponent,
])
    .component('ngbUserManagement', userManagementBtnComponent)
    .controller(controller.UID, controller)
    .name;
