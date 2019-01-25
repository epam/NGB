import BaseController from '../../../../baseController';
import {getUserAttributesString} from '../../internal/utilities';

const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbUserFormController extends BaseController {

    title = 'Group';
    roleId = null;
    isGroup = false;
    availableUsers = [];
    roleUsers = [];
    roleInitialUserIds = [];
    selectedUsers = [];
    errorMessages = [];

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
        return !this.isNewGroup && this.isGroup;
    }

    get isValid() {
        return this.groupName && this.groupName.length > 0;
    }

    get selectedUsersStr() {
        return (this.selectedUsers || []).map(u => u.name).join(', ');
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

        this.clearErrors();

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
        });

        if (roleId) {
            this.service.getRole(roleId).then(role => {
                if (role) {
                    this.groupName = isGroup && role.name.startsWith(ROLE_NAME_FIRST_PART)
                        ? role.name.slice(ROLE_NAME_FIRST_PART.length)
                        : role.name;
                    this.userDefault = role.userDefault;
                }
            });
        }

        this.service.getUsers((users) => {
            this.availableUsers = users.map(user => ({
                ...user,
                userAttributes: getUserAttributesString(user) || undefined
            }));
            if (roleId) {
                this.service.getRoleUsers(roleId, (users) => {
                    this.roleUsers = users.map(user => ({
                        ...user,
                        userAttributes: getUserAttributesString(user) || undefined
                    }));
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

    clearErrors() {
        this.errorMessages = [];
    }

    addError(error) {
        this.errorMessages.push(error);
        if (this.$scope !== null && this.$scope !== undefined) {
            this.$scope.$apply();
        }
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
        this.clearErrors();
        this.$mdDialog.hide();
    }

    cancel() {
        this.clearErrors();
        this.$mdDialog.cancel();
    }

    save() {
        this.clearErrors();
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
        const groupName = this.isGroup ? `${ROLE_NAME_FIRST_PART}${this.groupName}` : this.groupName;
        if (this.isNewGroup) {
            // create
            this.service.createGroup(groupName, this.userDefault, usersToAdd)
                .then(() => {
                    this.close();
                })
                .catch((error) => {
                    this.addError(error || 'An error occurred upon group creation');
                });
        } else {
            this.service.updateGroup(this.roleId, groupName, this.userDefault, usersToAdd, usersToRemove)
                .then(() => {
                    this.close();
                })
                .catch((error) => {
                    this.addError(error || `An error occurred upon ${this.isGroup ? 'group' : 'role'} update`);
                });
        }
    }

    delete() {
        this.clearErrors();
        const confirm = this.$mdDialog.confirm({skipHide: true})
            .title('Group will be deleted. Are you sure?')
            .textContent('This action can not be undone.')
            .ariaLabel('Delete group')
            .ok('OK')
            .cancel('Cancel');
        this.$mdDialog.show(confirm).then(() => {
            this.service.deleteGroup(this.roleId)
                .then(() => {
                    this.close();
                })
                .catch((error) => {
                    this.addError(error || `An error occurred upon ${this.isGroup ? 'group' : 'role'} deletion`);
                });
        });
    }

}
