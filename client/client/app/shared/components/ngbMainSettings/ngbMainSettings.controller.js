import ngbMainSettingsDlgController from './ngbMainSettingsDlg/ngbMainSettingsDlg.controller';
import angular from 'angular';

export default class ngbMainSettingsBtnController {
    static get UID() {
        return 'ngbMainSettingsBtnController';
    }

    /* @ngInject */
    constructor(localDataService, $mdDialog) {
        this._mdDialog = $mdDialog;
        this._localDataService = localDataService;
    }

    openDlg() {
        this._mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbMainSettingsDlgController,
            controllerAs: 'ctrl',
            parent: angular.element(document.body),
            template: require('./ngbMainSettingsDlg/ngbMainSettingsDlg.tpl.html'),
            locals: {
                settings: this._localDataService.getSettingsCopy()
            }
        });

    }
}
