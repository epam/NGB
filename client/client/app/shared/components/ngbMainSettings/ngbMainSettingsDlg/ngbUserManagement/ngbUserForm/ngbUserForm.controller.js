import BaseController from '../../../../../baseController';

export default class ngbUserFormController extends BaseController {

    title = 'User';
    userId = null;
    availableRoles = [];

    newUserName = '';
    selectedRoles = [];
    // searchTerm = '';
    formGridOptions = {};

    get dialogTitle() {
        return this.title || null;
    }

    get isNewUser() {
        return this.userId === null;
    }

    get currentRoles() {
        // todo
        // if (this.formGridOptions.data.length) {
        //     return this.formGridOptions.data.map(role => role.id);
        // } else {
        //     return [];
        // }
    }

    static get UID() {
        return 'ngbUserFormController';
    }

    /* @ngInject */
    constructor($mdDialog, $scope, ngbUserRoleFormService, ngbUserManagementGridOptionsConstant, title, userId) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            title,
            userId,
            service: ngbUserRoleFormService,
        });

        Object.assign(this.formGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.scope,
            columnDefs: this.service.getColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            },
            // showHeader: false,
        });

        this.service.getRoles((roles) => {
            this.availableRoles = roles;
            if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });

        if (!this.isNewUser) {
            this.fetchCurrentUserRolesData();
        } else {
            this.fetchDefaultRolesData();
        }

    }

    addRolesToGrid() {
        // todo
        // this.formGridOptions.data = [
        //     ...this.selectedRoles,
        //     ...this.formGridOptions.data,
        // ];
        // if (this.$scope !== null && this.$scope !== undefined) {
        //     this.$scope.$apply();
        // }
    }

    clearSearchTerm() {
        this.searchTerm = '';
    }

    fetchDefaultRolesData() {
        this.service.getDefaultRoles((gridData) => {
            this.formGridOptions.data = gridData;
            if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });
    }

    fetchCurrentUserRolesData() {
        if (!this.userId) {
            return;
        }
        this.service.getUsersGroupsAndRoles(this.userId, (gridData) => {
            this.formGridOptions.data = gridData;
            if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });
    }

    close() {
        this.$mdDialog.hide();
    }

    cancel() {
        this.$mdDialog.cancel();
    }

    save() {
        if (this.isNewUser) {
            // create
            this.service.createUser(this.newUserName, this.currentRoles, () => {
                this.close();
            });
        } else {
            // update
            this.service.saveUser(this.userId, this.currentRoles, () => {
                this.close();
            });
        }
    }

    delete() {
        const confirm = this.$mdDialog.confirm({skipHide: true})
            .title('User will be deleted. Are you sure?')
            .textContent('This action can not be undone.')
            .ariaLabel('Delete user')
            .ok('OK')
            .cancel('Cancel');
        this.$mdDialog.show(confirm).then(() => {
            this.service.deleteUser(this.userId, () => {
                this.close();
            });
        });
    }

}
