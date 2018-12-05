import angular from 'angular';
import BaseController from '../../../../baseController';

import ngbUserFormController from './ngbUserForm/ngbUserForm.controller';

export default class ngbUserManagementController extends BaseController {

    usersGridOptions = {};
    groupsGridOptions = {};
    rolesGridOptions = {};
    isUsersLoading = true;

    static get UID() {
        return 'ngbUserManagementController';
    }

    /* @ngInject */
    constructor($mdDialog, $scope, ngbUserManagementService, ngbUserManagementGridOptionsConstant) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            service: ngbUserManagementService,
        });
        // this.log = (log) => console.log(log);

        Object.assign(this.usersGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.scope,
            columnDefs: this.service.getUserManagementColumns(['User', 'Groups', 'Roles', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });
        Object.assign(this.groupsGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.scope,
            data: [],
            columnDefs: this.service.getUserManagementColumns(['Group', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });
        Object.assign(this.rolesGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.scope,
            data: [],
            columnDefs: this.service.getUserManagementColumns(['Role', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });

        this.fetchUsers();
        this.fetchRolesAndGroups();
    }

    fetchUsers() {
        this.isUsersLoading = true;
        this.service.getUsers((users) => {
            this.usersGridOptions.data = users;

            if (this.groupsGridOptions.data && this.rolesGridOptions.data) {
                this.isUsersLoading = false;
                if (this.scope !== null && this.scope !== undefined) {
                    this.scope.$apply();
                }
            }
        });
    }

    fetchRolesAndGroups() {
        this.isUsersLoading = true;
        this.service.getRolesAndGroups((groups, roles) => {
            this.groupsGridOptions.data = groups;
            this.rolesGridOptions.data = roles;

            if (this.usersGridOptions.data) {
                this.isUsersLoading = false;
                if (this.scope !== null && this.scope !== undefined) {
                    this.scope.$apply();
                }
            }
        });
    }

    openEditDialog(entity) {
        switch (entity.type.toLowerCase()) {
            case 'user':
                this.openEditUserDlg(entity);
                break;
            case 'group': {
                const group = {
                    ...entity,
                    name: `ROLE_${entity.groupName}`,
                };
                this.openEditRoleDlg(group);
                break;
            }
            case 'role': {
                const role = {
                    ...entity,
                    name: entity.roleName,
                };
                this.openEditRoleDlg(role);
                break;
            }
        }
    }

    openEditUserDlg(user) {
        console.log(user);
        this.$mdDialog.show({
            clickOutsideToClose: true,
            bindToController: true,
            controller: ngbUserFormController,
            controllerAs: 'ctrl',
            locals: {
                title: 'Edit user',
                userId: user.id,
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbUserForm/ngbUserForm.tpl.html'),
        }).then(() => {
            this.fetchUsers();
        });
    }

    openEditRoleDlg(role) {
        console.log(role);
        // todo
        // this.$mdDialog.show({
        //     clickOutsideToClose: true,
        //     controller: ngbUserFormController,
        //     controllerAs: 'ctrl',
        //     locals: {
        //         title: 'Edit user',
        //         user,
        //     },
        //     parent: angular.element(document.body),
        //     skipHide: true,
        //     template: require('./ngbUserForm/ngbUserForm.tpl.html'),
        // }).then(() => {
        //     this.fetchRolesAndGroups();
        // });
    }

    openCreateUserDlg() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbUserFormController,
            controllerAs: 'ctrl',
            locals: {
                title: 'Create user',
                userId: null,
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbUserForm/ngbUserForm.tpl.html'),
        }).then(() => {
            this.fetchUsers();
        });
    }

}

