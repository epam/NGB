
const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbUserManagementDlgService {

    _userDataService;
    _roleDataService;
    settings;

    static instance(userDataService, roleDataService) {
        return new ngbUserManagementDlgService(userDataService, roleDataService);
    }

    constructor(userDataService, roleDataService) {
        this._userDataService = userDataService;
        this._roleDataService = roleDataService;
    }

    getUsers(callback) {
        this._userDataService.getUsers().then((data) => {
            callback(this._mapUsersData(data));
        });
    }

    /*
        getGroups(callback) {
            this._roleDataService.getRoles().then((data) => {
                callback(this._mapGroupsData(data));
            });
        }

        getRoles(callback) {
            this._roleDataService.getRoles().then((data) => {
                callback(this._mapRolesData(data));
            });
        }
    */

    getRolesAndGroups(callback) {
        this._roleDataService.getRoles().then((data) => {
            callback(this._mapGroupsData(data), this._mapRolesData(data));
        });
    }

    _mapUsersData(usersData) {
        if (usersData && usersData.length) {
            return usersData.map(user => ({
                // todo
                editable: true,
                id: user.id,
                userName: user.userName,
                groups: (user.roles || []).map(role => {
                    const {id, predefined, name, userDefault} = role;
                    if (`${predefined}`.toLowerCase() === 'false') {
                        const groupName = name.includes(ROLE_NAME_FIRST_PART) ? name.slice(ROLE_NAME_FIRST_PART.length) : name;
                        return { id, name: groupName, userDefault };
                    }
                    return false;
                }).filter(i => !!i),
                roles: (user.roles || []).map(role => {
                    const {id, predefined, name, userDefault} = role;
                    if (predefined) {
                        return { id, name, userDefault };
                    }
                    return false;
                }).filter(i => !!i),
                type: 'user',
            }));
        }
        return [];
    }

    // todo
    _mapRolesData(rolesData) {
        if (rolesData && rolesData.length) {
            return rolesData.filter(role => `${role.predefined}`.toLowerCase() === 'true').map(role => ({
                id: +role.id,
                name: role.name,
                type: 'role',
                userDefault: role.userDefault,
            }));
        }
        return [];
    }

    _mapGroupsData(groupsData) {
        if (groupsData && groupsData.length) {
            return groupsData.filter(role => `${role.predefined}`.toLowerCase() === 'false').map(role => ({
                id: +role.id,
                name: role.name.includes(ROLE_NAME_FIRST_PART) ? role.name.slice(ROLE_NAME_FIRST_PART.length) : role.name,
                type: 'group',
                userDefault: role.userDefault,
            }));
        }
        return [];
    }

    getUserManagementColumns(columnsList) {
        const columnDefs = [];
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column.toLowerCase()) {
                case 'actions':
                    columnDefs.push({
                        cellTemplate: `
                            <div layout="row" style="flex-flow: row wrap; justify-content: center; align-items: center; width: 100%">
                                <md-button
                                    aria-label="Edit"
                                    class="md-mini md-hue-1 grid-action-button"
                                    ng-if="row.entity.editable"
                                    ng-click="grid.appScope.ctrl.openEditUserDlg(row.entity, $event)">
                                    <ng-md-icon icon="edit"></ng-md-icon>
                                </md-button>
                            </div>`,
                        enableColumnMenu: false,
                        enableSorting: false,
                        enableMove: false,
                        field: column.toLowerCase(),
                        maxWidth: 120,
                        minWidth: 120,
                        name: ''
                    });
                    break;
                case 'groups':
                    columnDefs.push({
                        cellTemplate: `
                            <div layout="row" ng-if="row.entity.groups.length <= 3" style="flex-flow: row wrap; align-items: center;">
                                <span
                                    ng-repeat="group in row.entity.groups track by $index"
                                    style="
                                        margin: 2px;
                                        padding: 2px 4px;
                                        border-radius: 5px;
                                        border: 1px solid #ddd;
                                        background-color: #fefefe;
                                        font-size: x-small;
                                        font-weight: bold;
                                        text-transform: uppercase;">
                                    {{group.name}}
                                </span>
                            </div>
                            <div layout="row" ng-if="row.entity.groups.length > 3" style="flex-flow: row wrap; align-items: center;">
                                <span
                                    ng-repeat="group in row.entity.groups.slice(0, 3) track by $index"
                                    style="
                                        margin: 2px;
                                        padding: 2px 4px;
                                        border-radius: 5px;
                                        border: 1px solid #ddd;
                                        background-color: #fefefe;
                                        font-size: x-small;
                                        font-weight: bold;
                                        text-transform: uppercase;">
                                    {{group.name}}
                                </span>
                                <ng-md-icon icon="more_horiz">
                                    <md-tooltip>
                                        <span
                                            ng-repeat="group in row.entity.groups track by $index"
                                            style="
                                                margin: 2px;
                                                padding: 2px 4px;
                                                border-radius: 5px;
                                                border: 1px solid #ddd;
                                                background-color: #fefefe;
                                                font-size: x-small;
                                                font-weight: bold;
                                                text-transform: uppercase;
                                                color: black;">
                                            {{group.name}}
                                        </span>
                                    </md-tooltip>
                                </ng-md-icon>
                            </div>
                        `,
                        enableColumnMenu: false,
                        enableSorting: false,
                        field: 'groups',
                        name: 'Groups',
                        width: '*',
                        minWidth: 50,
                    });
                    break;
                case 'roles':
                    columnDefs.push({
                        cellTemplate: `
                            <div layout="row" ng-if="row.entity.roles.length <= 3" style="flex-flow: row wrap; align-items: center;">
                                <span
                                    ng-repeat="role in row.entity.roles track by $index"
                                    style="
                                        margin: 2px;
                                        padding: 2px 4px;
                                        border-radius: 5px;
                                        border: 1px solid #ddd;
                                        background-color: #fefefe;
                                        font-size: x-small;
                                        font-weight: bold;
                                        text-transform: uppercase;">
                                    {{role.name}}
                                </span>
                            </div>
                            <div layout="row" ng-if="row.entity.roles.length > 3" style="flex-flow: row wrap; align-items: center;">
                                <span
                                    ng-repeat="role in row.entity.roles.slice(0, 3) track by $index"
                                    style="
                                        margin: 2px;
                                        padding: 2px 4px;
                                        border-radius: 5px;
                                        border: 1px solid #ddd;
                                        background-color: #fefefe;
                                        font-size: x-small;
                                        font-weight: bold;
                                        text-transform: uppercase;">
                                    {{role.name}}
                                </span>
                                <ng-md-icon icon="more_horiz">
                                    <md-tooltip>
                                        <span
                                            ng-repeat="role in row.entity.roles track by $index"
                                            style="
                                                margin: 2px;
                                                padding: 2px 4px;
                                                border-radius: 5px;
                                                border: 1px solid #ddd;
                                                background-color: #fefefe;
                                                font-size: x-small;
                                                font-weight: bold;
                                                text-transform: uppercase;
                                                color: black;">
                                            {{role.name}}
                                        </span>
                                    </md-tooltip>
                                </ng-md-icon>
                            </div>
                        `,
                        enableColumnMenu: false,
                        enableSorting: false,
                        field: 'roles',
                        name: 'Roles',
                        width: '*',
                        minWidth: 50,
                    });
                    break;
                case 'user':
                    columnDefs.push({
                        enableColumnMenu: false,
                        enableSorting: true,
                        field: 'userName',
                        minWidth: 50,
                        name: 'User',
                        width: '*',
                    });
                    break;
                case 'group':
                    columnDefs.push({
                        enableColumnMenu: false,
                        enableSorting: true,
                        field: 'groupName',
                        minWidth: 50,
                        name: 'Group',
                        width: '*',
                    });
                    break;
                case 'role':
                    columnDefs.push({
                        enableColumnMenu: false,
                        enableSorting: true,
                        field: 'roleName',
                        minWidth: 50,
                        name: 'Role',
                        width: '*',
                    });
                    break;
                default:
                    columnDefs.push({
                        field: column.toLowerCase(),
                        minWidth: 50,
                        name: column,
                        width: '*',
                    });
                    break;
            }
        }

        return columnDefs;
    }

    getGroupsManagementColumns() {
        return [
            {
                enableColumnMenu: false,
                enableSorting: true,
                field: 'name',
                minWidth: 50,
                name: 'Group',
                width: '*',
            },
            {
                cellTemplate: `
                            <div layout="row" style="flex-flow: row wrap; justify-content: center; align-items: center; width: 100%">
                                <md-button
                                    aria-label="Edit"
                                    class="md-mini md-hue-1 grid-action-button"
                                    ng-click="grid.appScope.ctrl.openEditGroupDlg(row.entity, $event)">
                                    <ng-md-icon icon="edit"></ng-md-icon>
                                </md-button>
                            </div>`,
                enableColumnMenu: false,
                enableSorting: false,
                enableMove: false,
                field: 'actions',
                maxWidth: 120,
                minWidth: 120,
                name: ''
            }
        ];
    }

    getRolesManagementColumns() {
        return [
            {
                enableColumnMenu: false,
                enableSorting: true,
                field: 'name',
                minWidth: 50,
                name: 'Role',
                width: '*',
            },
            {
                cellTemplate: `
                            <div layout="row" style="flex-flow: row wrap; justify-content: center; align-items: center; width: 100%">
                                <md-button
                                    aria-label="Edit"
                                    class="md-mini md-hue-1 grid-action-button"
                                    ng-click="grid.appScope.ctrl.openEditRoleDlg(row.entity, $event)">
                                    <ng-md-icon icon="edit"></ng-md-icon>
                                </md-button>
                               
                            </div>`,
                enableColumnMenu: false,
                enableSorting: false,
                enableMove: false,
                field: 'actions',
                maxWidth: 120,
                minWidth: 120,
                name: ''
            }
        ];
    }

}
