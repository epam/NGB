import angular from 'angular';
import ngbPermissionsFormController from '../ngbPermissionsForm/ngbPermissionsForm.controller';

export default class NgbDataSetContextMenuController {

    node;

    constructor($scope, $mdDialog, ngbDataSetContextMenu, userDataService) {
        this.$scope = $scope;
        this.node = $scope.node;
        this.$mdDialog = $mdDialog;
        this.ngbDataSetContextMenu = ngbDataSetContextMenu;
        this.userDataService = userDataService;
    }

    shouldOpenMenuPromise () {
        return this.userDataService.getCurrentUser()
            .then(user => user.hasRoles(this.node.roles || []));
    }

    openPermissions (event) {
        event.preventDefault();
        event.stopPropagation();
        if (this.ngbDataSetContextMenu.visible()) {
            this.ngbDataSetContextMenu.close();
        }
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbPermissionsFormController,
            controllerAs: 'ctrl',
            locals: {
                node: this.node
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('../ngbPermissionsForm/ngbPermissionsForm.template.html'),
        });
    }
}
