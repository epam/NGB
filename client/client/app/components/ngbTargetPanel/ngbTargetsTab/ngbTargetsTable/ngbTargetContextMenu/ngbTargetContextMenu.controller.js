import angular from 'angular';
import ngbTargetPermissionsFormController from './ngbTargetPermissionsForm/ngbTargetPermissionsForm.controller';

const ROLE_ADMIN = 'ROLE_ADMIN';

export default class ngbTargetContextMenuController {

    hasRole = false;

    get adminRole() {
        return ROLE_ADMIN;
    }

    static get UID() {
        return 'ngbTargetContextMenuController';
    }

    constructor($scope, $mdDialog, ngbTargetContextMenu, utilsDataService, userDataService) {
        Object.assign(this, {$scope, $mdDialog, ngbTargetContextMenu, utilsDataService, userDataService});
        this.checkUserRole();
    }

    checkUserRole() {
        this.utilsDataService.isRoleModelEnabled()
            .then(utilsDataService => {
                if (utilsDataService) {
                    this.userDataService.getCurrentUser()
                        .then(user => {
                            this.hasRole = user.hasRoles([this.adminRole]);
                        });
                } else {
                    this.hasRole = false;
                }
            });
    }

    openPermissions(event) {
        event.preventDefault();
        event.stopPropagation();
        if (this.ngbTargetContextMenu.visible()) {
            this.ngbTargetContextMenu.close();
        }
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbTargetPermissionsFormController,
            controllerAs: '$ctrl',
            locals: {
                target: this.$scope.row.entity
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbTargetPermissionsForm/ngbTargetPermissionsForm.tpl.html'),
        });
    }
}