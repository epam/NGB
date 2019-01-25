import {getUserAttributesString} from '../internal/utilities';

const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbUserRoleFormService {

    static instance(userDataService, roleDataService) {
        return new ngbUserRoleFormService(userDataService, roleDataService);
    }

    constructor(userDataService, roleDataService) {
        this._userDataService = userDataService;
        this._roleDataService = roleDataService;
    }

    getDefaultRoles(callback) {
        this._roleDataService.getRoles().then((data) => {
            callback(this._mapDefaultRolesData(data));
        });
    }

    getRoles(callback) {
        this._roleDataService.getRoles().then((data) => {
            callback(this._mapGroupsAndRolesData(data));
        });
    }

    getUsers(callback) {
        this._userDataService.getUsers().then((users) => {
            callback(this._mapUsers(users));
        });
    }

    getUsersGroupsAndRoles(userId, callback) {
        this.getUser(userId).then((user) => {
            callback(this._mapUserRoles(user), user.userName);
        });
    }

    getRoleUsers(roleId, callback) {
        this.getRole(roleId).then((role) => {
            callback(this._mapRoleUsers(role));
        });
    }

    getUser(id) {
        return this._userDataService.getUser(id);
    }

    getRole(id) {
        return this._roleDataService.getRole(id);
    }

    _mapDefaultRolesData(rolesData) {
        return this._mapGroupsAndRolesData(rolesData).filter(role => role.userDefault === true);
    }

    _mapGroupsAndRolesData(rolesData) {
        return rolesData.map(role => ({
            id: role.id,
            name: !role.predefined && role.name.includes(ROLE_NAME_FIRST_PART) ? role.name.slice(ROLE_NAME_FIRST_PART.length) : role.name,
            predefined: role.predefined,
            userDefault: role.userDefault,
        }));
    }

    _mapUserRoles(userData) {
        return (userData.roles || []).map(role => ({
            deletable: true,
            id: role.id,
            name: !role.predefined && role.name.includes(ROLE_NAME_FIRST_PART) ? role.name.slice(ROLE_NAME_FIRST_PART.length) : role.name,
            predefined: role.predefined,
            userDefault: role.userDefault,
        }));
    }

    _mapUsers(users) {
        return users.map(user => ({
            attributes: user.attributes,
            deletable: true,
            id: user.id,
            name: user.userName,
            userAttributes: user.attributes ? getUserAttributesString(user) : undefined
        }));
    }

    _mapRoleUsers(roleData) {
        if (roleData) {
            return this._mapUsers(roleData.users || []);
        } else {
            return [];
        }
    }

    createUser(name, roles) {
        return this._userDataService.createUser({
            userName: name,
            roleIds: roles,
        });
    }

    createGroup(name, userDefault, users) {
        return this._roleDataService.createGroup(name, userDefault)
          .then(group => {
              if (group && users.length > 0) {
                  return this._roleDataService.assignUsersToRole(group.id, users);
              } else if (group) {
                  return Promise.resolve(group);
              } else {
                  return Promise.resolve(null);
              }
          });
    }

    updateGroup(roleId, name, userDefault, usersToAdd, usersToRemove) {
        return this._roleDataService.updateRole(roleId, {name, userDefault,})
            .then(group => {
                if (group) {
                    if (usersToAdd.length > 0) {
                        return this._roleDataService.assignUsersToRole(group.id, usersToAdd)
                            .then(() => {
                                if (usersToRemove.length > 0) {
                                    return this._roleDataService.removeRoleFromUsers(group.id, usersToRemove)
                                        .then(() => Promise.resolve(group));
                                } else {
                                    return Promise.resolve(group);
                                }
                            });
                    } else if (usersToRemove.length > 0) {
                        return this._roleDataService.removeRoleFromUsers(group.id, usersToRemove)
                            .then(() => Promise.resolve(group));
                    } else {
                        return Promise.resolve(group);
                    }
                } else {
                    return Promise.resolve(null);
                }
            });
    }

    saveUser(id, userName, roles) {
        return this._userDataService.updateUser(id, {userName, roleIds: roles});
    }

    deleteUser(id) {
        return this._userDataService.deleteUser(id);
    }

    deleteGroup(id) {
        return this._roleDataService.deleteRole(id);
    }

    getRolesColumns() {
        return [{
            enableColumnMenu: false,
            enableSorting: true,
            field: 'name',
            minWidth: 50,
            name: 'Name',
            width: '*',
        }, {
            cellTemplate: `
                <div layout="row" style="flex-flow: row wrap; justify-content: center; align-items: center; width: 100%">
                  <md-button
                      aria-label="Delete"
                      class="md-mini md-hue-1 grid-action-button"
                      ng-if="row.entity.deletable"
                      ng-click="grid.appScope.ctrl.removeRoleFromGrid(row.entity.id, $event)">
                      <ng-md-icon icon="delete"></ng-md-icon>
                  </md-button>
                </div>`,
            enableColumnMenu: false,
            enableSorting: false,
            enableMove: false,
            field: 'actions',
            maxWidth: 120,
            minWidth: 120,
            name: ''
        }];
    }

    getUserInfo(userName) {
        return this._userDataService.getCachedUsers()
            .then(users => {
                const [user] = (users || []).filter(u => u.userName === userName);
                if (user) {
                    return user;
                }
                return null;
            });
    }

    getUsersColumns() {
        return [{
            cellTemplate: `
                <div class="ui-grid-cell-contents">
                    <span>
                        {{row.entity.name}}
                        <md-tooltip ng-if="row.entity.userAttributes">
                            {{row.entity.userAttributes}}
                        </md-tooltip>
                    </span>
                </div>
            `,
            enableColumnMenu: false,
            enableSorting: true,
            field: 'name',
            minWidth: 50,
            name: 'Name',
            width: '*',
        }, {
            cellTemplate: `
                <div layout="row" style="flex-flow: row wrap; justify-content: center; align-items: center; width: 100%">
                    <md-button
                        aria-label="Delete"
                        class="md-mini md-hue-1 grid-action-button"
                        ng-click="grid.appScope.ctrl.removeUser(row.entity, $event)">
                        <ng-md-icon icon="delete"></ng-md-icon>
                    </md-button>
                </div>`,
            enableColumnMenu: false,
            enableSorting: false,
            enableMove: false,
            field: 'actions',
            maxWidth: 120,
            minWidth: 120,
            name: ''
        }];
    }

}
