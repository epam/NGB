import angular from 'angular';

export default class ngbGenesTableDownloadController {

    constructor($mdDialog) {
        this.$mdDialog = $mdDialog;
    }

    static get UID() {
        return 'ngbGenesTableDownloadController';
    }

    openDlg() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: 'ngbVariantsTableDownloadDlgController',
            controllerAs: 'ctrl',
            parent: angular.element(document.body),
            template: require('./ngbGenesTableDownloadDlg.tpl.html')
        });

    }

}
