import angular from 'angular';

export default class ngbBookmarkController {

    static get UID() {
        return 'ngbBookmarkController';
    }

    constructor(
        $mdDialog,
    ) {
        Object.assign(this, {
            $mdDialog,
        });
    }

    openDlg() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: 'ngbBookmarkSaveDlgController',
            controllerAs: 'ctrl',
            parent: angular.element(document.body),
            template: require('./ngbBookmarkSaveDlg.tpl.html')
        });
    }
}
