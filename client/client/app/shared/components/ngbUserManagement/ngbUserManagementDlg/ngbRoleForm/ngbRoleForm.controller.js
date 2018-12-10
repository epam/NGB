import BaseController from '../../../../baseController';

export default class ngbUserFormController extends BaseController {

    title = 'Group';
    roleId = null;
    isGroup = false;
    availableUsers = [];
    roleUsers = [];
    roleInitialUserIds = [];
    selectedUsers = [];

    groupName = '';
    userDefault = false;
    formGridOptions = {};
    searchTerm = '';

    get dialogTitle() {
        return this.title || null;
    }

    get isNewGroup() {
        return this.roleId === null;
    }

    get deletable() {
        return !this.isNewGroup && this.isGroup; // todo
    }

    get isValid() {
        return this.groupName && this.groupName.length > 0;
    }

    static get UID() {
        return 'ngbRoleFormController';
    }

    /* @ngInject */
    constructor($mdDialog, $scope, ngbUserRoleFormService, ngbUserManagementGridOptionsConstant, title, roleId, isGroup) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            title,
            roleId,
            isGroup,
            service: ngbUserRoleFormService,
        });

        const self = this;

        $scope.searchFilter = function (item) {
            return !self.searchTerm || (item.name || '').toLowerCase().indexOf(self.searchTerm.toLowerCase()) >= 0;
        };

        Object.assign(this.formGridOptions, {
            ...ngbUserManagementGridOptionsConstant,
            appScopeProvider: this.scope,
            columnDefs: this.service.getUsersColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            },
            // showHeader: false,
        });

        if (roleId) {
            this.service.getRole(roleId).then(role => {
                if (role) {
                    this.groupName = role.name;
                    this.userDefault = role.userDefault;
                }
            });
        }

        this.service.getUsers((users) => {
            this.availableUsers = users;
            if (roleId) {
                this.service.getRoleUsers(roleId, (users) => {
                    this.roleUsers = users;
                    this.roleInitialUserIds = users.map(u => +u.id);
                    for (let i = 0; i < users.length; i++) {
                        const [user] = this.availableUsers.filter(u => +u.id === +users[i].id);
                        if (user) {
                            const index = this.availableUsers.indexOf(user);
                            this.availableUsers.splice(index, 1);
                        }
                    }
                    this.formGridOptions.data = this.roleUsers;
                    if (this.$scope !== null && this.$scope !== undefined) {
                        this.$scope.$apply();
                    }
                });
            } else {
                this.formGridOptions.data = this.roleUsers;
                if (this.$scope !== null && this.$scope !== undefined) {
                    this.$scope.$apply();
                }
            }
        });
    }

    addUsersToGrid() {
        this.formGridOptions.data = [
            ...this.selectedUsers,
            ...this.formGridOptions.data,
        ];
        for (let i = 0; i < this.selectedUsers.length; i++) {
            const [user] = this.availableUsers.filter(u => +u.id === +this.selectedUsers[i].id);
            if (user) {
                const index = this.availableUsers.indexOf(user);
                this.availableUsers.splice(index, 1);
            }
        }
        this.selectedUsers = [];
    }

    removeUser(user) {
        const index = this.formGridOptions.data.indexOf(user);
        this.formGridOptions.data.splice(index, 1);
        this.availableUsers.push(user);
    }

    clearSearchTerm() {
        this.searchTerm = '';
    }

    close() {
        this.$mdDialog.hide();
    }

    cancel() {
        this.$mdDialog.cancel();
    }

    save() {
        const usersToAdd = [];
        const usersToRemove = [];
        const list = (this.formGridOptions.data || []).map(u => +u.id);
        const roleList = this.roleInitialUserIds;
        for (let i = 0; i < roleList.length; i++) {
            if (list.indexOf(roleList[i]) === -1) {
                usersToRemove.push(roleList[i]);
            }
        }
        for (let i = 0; i < list.length; i++) {
            if (roleList.indexOf(list[i]) === -1) {
                usersToAdd.push(list[i]);
            }
        }
        if (this.isNewGroup) {
            // create
            this.service.createGroup(this.groupName, this.userDefault, usersToAdd, () => {
                this.close();
            });
        } else {
            this.service.updateGroup(this.roleId, this.groupName, this.userDefault, usersToAdd, usersToRemove, () => {
                this.close();
            });
        }
    }

    delete() {
        const confirm = this.$mdDialog.confirm({skipHide: true})
            .title('Group will be deleted. Are you sure?')
            .textContent('This action can not be undone.')
            .ariaLabel('Delete group')
            .ok('OK')
            .cancel('Cancel');
        this.$mdDialog.show(confirm).then(() => {
            this.service.deleteGroup(this.roleId, () => {
                this.close();
            });
        });
    }

}
