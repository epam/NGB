
const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbUserFormService {

    static instance(userDataService, roleDataService) {
        return new ngbUserFormService(userDataService, roleDataService);
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

    getUsersGroupsAndRoles(userId, callback) {
        this.getUser(userId).then((user) => {
            callback(this._mapUserRoles(user));
        });
    }

    getUser(id) {
        return this._userDataService.getUser(id);
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
        return userData.roles.map(role => ({
            deletable: true,
            id: role.id,
            name: !role.predefined && role.name.includes(ROLE_NAME_FIRST_PART) ? role.name.slice(ROLE_NAME_FIRST_PART.length) : role.name,
            userDefault: role.userDefault,
        }));
    }

    createUser(name, roles, callback) {
        this._userDataService.createUser({
            name,
            roles,
        }).then(res => {
            callback(res);
        });
    }

    saveUser(id, roles, callback) {
        this._userDataService.updateUser(id, {roles}).then(res => {
            callback(res);
        });
    }

    deleteUser(id, callback) {
        this._userDataService.deleteUser(id).then((res) => {
            callback(res);
        });
    }

    getColumns() {
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
                        class="md-fab md-mini md-hue-1"
                        ng-if="row.entity.deletable"
                        ng-click="grid.appScope.ctrl.removeRole(row.entity, $event)">
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
