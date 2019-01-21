import angular from 'angular';
import BaseController from '../../../baseController';

import ngbRoleFormController from './ngbRoleForm/ngbRoleForm.controller';
import ngbUserFormController from './ngbUserForm/ngbUserForm.controller';

export default class ngbUserManagementDlgController extends BaseController {

    usersGridOptions = {};
    groupsGridOptions = {};
    rolesGridOptions = {};
    isUsersLoading = true;

    popoverRef = null;

    usersSearchTerm = '';
    groupsSearchTerm = '';
    rolesSearchTerm = '';

    static get UID() {
        return 'ngbUserManagementDlgController';
    }

    /* @ngInject */
    constructor($mdDialog, $mdPanel, $scope, ngbUserManagementService, ngbUserManagementGridOptionsConstant) {
        super();
        Object.assign(this, {
            $mdDialog,
            $mdPanel,
            $scope,
            service: ngbUserManagementService,
        });

        Object.assign(this.usersGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.$scope,
            columnDefs: this.service.getUserManagementColumns(['User', 'Groups', 'Roles', 'Actions']),
            onRegisterApi: (gridApi) => {
                this.usersGridApi = gridApi;
                this.usersGridApi.core.handleWindowResize();
            }
        });
        Object.assign(this.groupsGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.$scope,
            data: [],
            columnDefs: this.service.getGroupsManagementColumns(),
            onRegisterApi: (gridApi) => {
                this.groupsGridApi = gridApi;
                this.groupsGridApi.core.handleWindowResize();
            }
        });
        Object.assign(this.rolesGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.$scope,
            data: [],
            columnDefs: this.service.getRolesManagementColumns(),
            onRegisterApi: (gridApi) => {
                this.rolesGridApi = gridApi;
                this.rolesGridApi.core.handleWindowResize();
            }
        });

        this.fetchAll();
    }

    close() {
        this.$mdDialog.hide();
    }

    fetchAll() {
        Promise.all([this.fetchUsers(), this.fetchRolesAndGroups()]).then(() => {
            this.isUsersLoading = false;
            if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });
    }

    fetchUsers() {
        this.isUsersLoading = true;
        return this.service.getUsers().then((users) => {
            this.usersGridOptions.data = users;
            this.isUsersLoading = false;
            if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });
    }

    fetchRolesAndGroups() {
        this.isUsersLoading = true;
        return this.service.getRolesAndGroups().then(({groups, roles}) => {
            this.groupsGridOptions.data = groups;
            this.rolesGridOptions.data = roles;
            this.isUsersLoading = false;
            if (this.$scope !== null && this.$scope !== undefined) {
                this.$scope.$apply();
            }
        });
    }

    usersSearchChanged() {
        if (!this.usersGridApi || !this.usersGridApi.grid) {
            return;
        }

        (this.usersGridApi.grid.columns || []).forEach(col => {
            if (col.field === 'userName') {
                col.filters[0].term = this.usersSearchTerm;
            }
        });
        if (this.$scope !== null && this.$scope !== undefined) {
            this.$scope.$apply();
        }
    }

    groupsSearchChanged() {
        if (!this.groupsGridApi || !this.groupsGridApi.grid) {
            return;
        }

        (this.groupsGridApi.grid.columns || []).forEach(col => {
            if (col.field === 'name') {
                col.filters[0].term = this.groupsSearchTerm;
            }
        });
        if (this.$scope !== null && this.$scope !== undefined) {
            this.$scope.$apply();
        }
    }

    showPopover(items, $ev) {
        $ev.stopImmediatePropagation();
        if (this.popoverRef) {
            return;
        }

        const viewportHeight = angular.element(document).height();
        const viewportWidth = angular.element(document).width();
        const position = this.$mdPanel.newPanelPosition()
            .relativeTo($ev.target)
            .addPanelPosition(
                viewportWidth / 2 > $ev.clientX
                    ? this.$mdPanel.xPosition.ALIGN_START
                    : this.$mdPanel.xPosition.ALIGN_END,
                viewportHeight / 2 > $ev.clientY
                    ? this.$mdPanel.yPosition.BELOW
                    : this.$mdPanel.yPosition.ABOVE);
        const panelAnimation = this.$mdPanel.newPanelAnimation()
            .openFrom($ev.target)
            .closeTo($ev.target)
            .withAnimation(this.$mdPanel.animation.FADE);
        const config = {
            animation: panelAnimation,
            attachTo: angular.element(document.body),
            clickOutsideToClose: true,
            escapeToClose: true,
            focusOnOpen: false,
            openFrom: $ev,
            panelClass: 'simple-popover',
            position: position,
            template: items.map(item => `<span class="group-role-tag${item.isAD ? ' ad-group' : ''}">${item.name}</span>`).join('')
        };
        this.$mdPanel.open(config).then(popover => {
            this.popoverRef = popover;
            this.popoverRef.panelEl.on('mouseover', (event) => {
                event.stopPropagation();
            });
            angular.element(document).one('mouseover', () => {
                this.popoverRef.close().then(() => {
                    this.popoverRef.destroy();
                    this.popoverRef = null;
                });
            });
        });
    }

    rolesSearchChanged() {
        if (!this.rolesGridApi || !this.rolesGridApi.grid) {
            return;
        }

        (this.rolesGridApi.grid.columns || []).forEach(col => {
            if (col.field === 'name') {
                col.filters[0].term = this.rolesSearchTerm;
            }
        });
        if (this.$scope !== null && this.$scope !== undefined) {
            this.$scope.$apply();
        }
    }

    openEditUserDlg(user) {
        this.$mdDialog.show({
            clickOutsideToClose: true,
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

    openCreateGroupDlg() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbRoleFormController,
            controllerAs: 'ctrl',
            locals: {
                isGroup: true,
                roleId: null,
                title: 'Create group'
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbRoleForm/ngbRoleForm.tpl.html'),
        }).then(() => {
            this.fetchAll();
        });
    }

    openEditGroupDlg(group) {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbRoleFormController,
            controllerAs: 'ctrl',
            locals: {
                isGroup: true,
                roleId: group.id,
                title: 'Edit group'
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbRoleForm/ngbRoleForm.tpl.html'),
        }).then(() => {
            this.fetchAll();
        });
    }

    openEditRoleDlg(role) {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbRoleFormController,
            controllerAs: 'ctrl',
            locals: {
                isGroup: false,
                roleId: role.id,
                title: 'Edit role'
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbRoleForm/ngbRoleForm.tpl.html'),
        }).then(() => {
            this.fetchAll();
        });
    }

}

