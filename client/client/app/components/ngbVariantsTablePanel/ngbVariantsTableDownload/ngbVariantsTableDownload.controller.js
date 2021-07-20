import angular from 'angular';

export default class ngbVariantsTableDownloadController {

    constructor($mdDialog) {
        this.$mdDialog = $mdDialog;
    }

    static get UID() {
        return 'ngbVariantsTableDownloadController';
    }

    openDlg() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: 'ngbVariantsTableDownloadDlgController',
            controllerAs: 'ctrl',
            parent: angular.element(document.body),
            template: require('./ngbVariantsTableDownloadDlg.tpl.html')
        });

    }

}
