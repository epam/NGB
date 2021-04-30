import BaseController from '../../../../baseController';

export default class ngbUserFormController extends BaseController {

    title = 'User';
    userId = null;
    availableRoles = [];
    errorMessages = [];

    userName = '';
    selectedRoles = [];
    searchTerm = '';
    formGridOptions = {};

    get dialogTitle() {
        return this.title || null;
    }

    get isNewUser() {
        return this.userId === null;
    }

    get currentRolesIds() {
        if (this.formGridOptions.data.length) {
            return this.formGridOptions.data.map(role => role.id);
        } else {
            return [];
        }
    }

    get isValid() {
        return this.userName && this.userName.length > 0;
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

        this.clearErrors();

        Object.assign(this.formGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.scope,
            columnDefs: this.service.getRolesColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            },
        });

        if (!this.isNewUser) {
            this.fetchCurrentUserRolesData(this.fetchAvailableRoles.bind(this));
        } else {
            this.fetchDefaultRolesData(this.fetchAvailableRoles.bind(this));
        }

    }

    clearErrors() {
        this.errorMessages = [];
    }

    addError(error) {
        this.errorMessages.push(error);
        if (this.$scope !== null && this.$scope !== undefined) {
            this.$scope.$apply();
        }
    }

    removeRoleFromGrid(id) {
        this.formGridOptions.data = this.formGridOptions.data.filter(r => r.id !== id);
        this.fetchAvailableRoles();
    }

    addRolesToGrid() {
        this.formGridOptions.data = [
            ...this.selectedRoles.map(r => ({
                ...r,
                deletable: true,
            })),
            ...this.formGridOptions.data,
        ];
        this.selectedRoles = [];
        this.fetchAvailableRoles();
    }

    clearSearchTerm() {
        this.searchTerm = '';
    }

    fetchAvailableRoles() {
        this.service.getRoles((roles) => {
            this.availableRoles = roles.filter(role => !this.currentRolesIds.length || !this.currentRolesIds.includes(role.id));
            if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });
    }

    fetchDefaultRolesData(callback) {
        this.service.getDefaultRoles((gridData) => {
            this.formGridOptions.data = gridData;
            if (callback) {
                callback();
            } else if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });
    }

    fetchCurrentUserRolesData(callback) {
        if (!this.userId) {
            return;
        }
        this.service.getUsersGroupsAndRoles(this.userId, (gridData, userName) => {
            this.formGridOptions.data = gridData;
            this.userName = userName;
            if (callback) {
                callback();
            } else if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });
    }

    close() {
        this.clearErrors();
        this.$mdDialog.hide();
    }

    cancel() {
        this.clearErrors();
        this.$mdDialog.cancel();
    }

    save() {
        this.clearErrors();
        if (this.isNewUser) {
            // create
            this.service.createUser(this.userName, this.currentRolesIds)
                .then(() => {
                    this.close();
                })
                .catch((error) => {
                    this.addError(error || 'An error occurred upon user creation');
                });
        } else {
            // update
            this.service.saveUser(this.userId, this.userName, this.currentRolesIds)
                .then(() => {
                    this.close();
                })
                .catch((error) => {
                    this.addError(error || 'An error occurred upon user update');
                });
        }
    }

    delete() {
        this.clearErrors();
        const confirm = this.$mdDialog.confirm({skipHide: true})
            .title('User will be deleted. Are you sure?')
            .textContent('This action can not be undone.')
            .ariaLabel('Delete user')
            .ok('OK')
            .cancel('Cancel');
        this.$mdDialog.show(confirm).then(() => {
            this.service.deleteUser(this.userId)
                .then(() => {
                    this.close();
                })
                .catch((error) => {
                    this.addError(error || 'An error occurred upon user deletion');
                });

        });
    }

}
