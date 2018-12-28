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
        const getUserAttributesString = (user) => {
            const values = [];
            const firstAttributes = ['FirstName', 'LastName'];
            for (const key in user.attributes) {
                if (user.attributes.hasOwnProperty(key) && firstAttributes.indexOf(key) >= 0) {
                    values.push(user.attributes[key]);
                }
            }
            for (const key in user.attributes) {
                if (user.attributes.hasOwnProperty(key) && firstAttributes.indexOf(key) === -1) {
                    values.push(user.attributes[key]);
                }
            }
            return values.join(' ');
        };
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

    createUser(name, roles, callback) {
        this._userDataService.createUser({
            userName: name,
            roleIds: roles,
        }).then(res => {
            callback(res);
        });
    }

    createGroup(name, userDefault, users, callback) {
        this._roleDataService.createGroup(name, userDefault)
          .then(group => {
              if (group && users.length > 0) {
                  this._roleDataService.assignUsersToRole(group.id, users)
                    .then(res => callback(res));
              } if (group) {
                  callback(group);
              } else {
                  callback(null);
              }
          });
    }

    updateGroup(roleId, name, userDefault, usersToAdd, usersToRemove, callback) {
        this._roleDataService.updateRole(roleId, {name, userDefault,})
          .then(group => {
              if (group) {
                  if (usersToAdd.length > 0) {
                      this._roleDataService.assignUsersToRole(group.id, usersToAdd)
                          .then(() => {
                              if (usersToRemove.length > 0) {
                                  this._roleDataService.removeRoleFromUsers(group.id, usersToRemove)
                                      .then(() => callback(group));
                              } else {
                                  callback(group);
                              }
                          });
                  } else if (usersToRemove.length > 0) {
                      this._roleDataService.removeRoleFromUsers(group.id, usersToRemove)
                          .then(() => callback(group));
                  } else {
                      callback(group);
                  }
              } else {
                  callback(null);
              }
          });
    }

    saveUser(id, userName, roles, callback) {
        this._userDataService.updateUser(id, {userName, roleIds: roles}).then(res => {
            callback(res);
        });
    }

    deleteUser(id, callback) {
        this._userDataService.deleteUser(id).then((res) => {
            callback(res);
        });
    }

    deleteGroup(id, callback) {
        this._roleDataService.deleteRole(id).then((res) => {
            callback(res);
        });
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

    getUsersColumns() {
        return [{
            cellTemplate: '<div class="ui-grid-cell-contents"><ngb-user user="row.entity.name" /></div>',
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
