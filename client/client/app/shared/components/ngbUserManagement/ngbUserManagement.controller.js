import ngbUserManagementDlgController from './ngbUserManagementDlg/ngbUserManagementDlg.controller';
import angular from 'angular';

export default class ngbUserManagementBtnController {
    static get UID() {
        return 'ngbUserManagementBtnController';
    }

    /* @ngInject */
    constructor(localDataService, $mdDialog) {
        this._mdDialog = $mdDialog;
        this._localDataService = localDataService;
    }

    openDlg() {
        this._mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbUserManagementDlgController,
            controllerAs: 'ctrl',
            fullscreen: true,
            parent: angular.element(document.body),
            template: require('./ngbUserManagementDlg/ngbUserManagementDlg.tpl.html'),
        });

    }
}
