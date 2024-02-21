import angular from 'angular';
import {
    getUserAttributesString,
    getUserDisplayNameString,
} from '../../../../../../shared/components/ngbUserManagement/internal/utilities';
import roleModel from '../../../../../../shared/utils/roleModel';
import ngbTargetAddUserRoleDlgController from './ngbTargetAddUserRoleDlg.controller';

const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbTargetPermissionsFormController {

    gridOptions = {
        height: '100%',
        headerRowHeight: 20,
        rowHeight: 30,
        showHeader: true,
        multiSelect: false,
        enableGridMenu: false,
        enableRowSelection: true,
        enableRowHeaderSelection: false,
        enableFiltering: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        treeRowHeaderAlwaysVisible: false,
        saveWidths: true,
        saveOrder: true,
        saveScroll: false,
        saveFocus: false,
        saveVisible: true,
        saveSort: true,
        saveFilter: true,
        savePinning: true,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveTreeView: false,
        saveSelection: false,
        infiniteScrollRowsFromEnd: 10,
        infiniteScrollUp: true,
        infiniteScrollDown: true,
    };

    target;
    users;
    roles;
    owner;
    mask;
    _selectedOwner;
    permissions = [];

    ownerSearchTerm;
    formGridOptions = {};

    _subject = null;
    _subjectPermissions = {
        readAllowed: false,
        readDenied: false,
        writeAllowed: false,
        writeDenied: false
    };

    get subject() {
        return this._subject;
    }

    get subjectPermissions() {
        return this._subjectPermissions;
    }

    get selectedOwner() {
        if (this._selectedOwner) {
            return this._selectedOwner;
        }
        return this.owner;
    }

    set selectedOwner(value) {
        this._selectedOwner = value;
    }

    get ownerChangeAllowed() {
        return roleModel.isOwner({mask: this.mask});
    }

    get permissionsChangeAllowed() {
        return roleModel.writeAllowed({mask: this.mask});
    }

    get ownerChanged() {
        return this._selectedOwner && this._selectedOwner !== this.owner;
    }

    get availableUsers() {
        return (this.users || []).filter(u => (this.permissions || [])
            .filter(p => p.principal && (p.name || '')
            .toLowerCase() === (u.userName || '')
            .toLowerCase()).length === 0);
    }

    get availableRoles() {
        return (this.roles || []).filter(r => (this.permissions || [])
            .filter(p => !p.principal && (p.name || '')
            .toLowerCase() === (r.name || '')
            .toLowerCase()).length === 0);
    }

    static get UID() {
        return 'ngbTargetPermissionsFormController';
    }

    static getRoleDisplayName(role) {
        if (!role.predefined && role.name.toUpperCase().startsWith(ROLE_NAME_FIRST_PART)) {
            return role.name.substring(ROLE_NAME_FIRST_PART.length);
        }
        return role.name;
    }

    constructor($scope, $mdDialog, dispatcher, target, ngbTargetPermissionsFormService) {
        Object.assign(this, {$scope, $mdDialog, dispatcher, target, ngbTargetPermissionsFormService});
        Object.assign(this.formGridOptions, {
            ...this.gridOptions,
            appScopeProvider: this.$scope,
            columnDefs: this.getPermissionsColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(
                    this.$scope,
                    (row) => this.selectPermissionSubject(row)
                );
            },
        });

        this.fetchPermissions();
        this.ngbTargetPermissionsFormService.getUsers()
            .then(users => {
                this.users = users || [];
            });
        this.ngbTargetPermissionsFormService.getRoles()
            .then(roles => {
                this.roles = roles || [];
                if (this.permissions) {
                    this.setPermissionsGridData(this.permissions);
                }
            });
    }

    getPermissionsColumns() {
        return [{
            cellTemplate: `
                    <div layout="row" style="flex-flow: row wrap; justify-content: center; align-items: center; width: 100%">
                        <ng-md-icon ng-if="row.entity.principal" icon="person"></ng-md-icon>
                        <ng-md-icon ng-if="!row.entity.principal" icon="group"></ng-md-icon>
                    </div>
                `,
            enableColumnMenu: false,
            enableMove: false,
            enableSorting: false,
            field: 'principal',
            maxWidth: 50,
            minWidth: 50,
            name: ' ',
        }, {
            cellTemplate: `
                <div class="ui-grid-cell-contents">
                    <span ng-if="row.entity.principal">
                        {{row.entity.displayName}}
                        <md-tooltip ng-if="row.entity.userAttributes">
                            {{row.entity.userAttributes}}
                        </md-tooltip>
                    </span>
                    <span ng-if="!row.entity.principal">{{row.entity.displayName}}</span>
                </div>
            `,
            enableColumnMenu: false,
            enableSorting: true,
            field: 'displayName',
            minWidth: 50,
            name: 'Name',
            width: '*',
        }, {
            cellTemplate: `
                      <div layout="row" style="flex-flow: row wrap; justify-content: center; align-items: center; width: 100%">
                          <md-button
                              ng-disabled="!grid.appScope.$ctrl.permissionsChangeAllowed"
                              aria-label="Delete"
                              class="md-mini md-hue-1 grid-action-button"
                              ng-click="grid.appScope.$ctrl.deleteSubjectPermissions(row.entity, $event)">
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

    selectPermissionSubject(row, scopeApply = true) {
        let subject = null;
        if (row && row.isSelected) {
            subject = row.entity;
        }
        this._subject = subject;
        this._subjectPermissions = {
            readAllowed: roleModel.readAllowed(subject, true),
            readDenied: roleModel.readDenied(subject, true),
            writeAllowed: roleModel.writeAllowed(subject, true),
            writeDenied: roleModel.writeDenied(subject, true)
        };
        if (scopeApply) {
            this.$scope.$apply();
        }
    }

    fetchPermissions() {
        this.ngbTargetPermissionsFormService.getPermissions(this.target)
            .then(data => {
                if (data) {
                    this.owner = (data.owner || '').toUpperCase();
                    this.mask = data.mask;
                    this.permissions = data.permissions;
                    this.setPermissionsGridData(this.permissions);
                }
            });
    }

    async setPermissionsGridData(data) {
        if (this.subject && !data.map(i => i.name).includes(this.subject.name)) {
            this._subject = null;
        }
        const mapGridData = async (permission) => {
            let displayName;
            let userAttributes;
            if (permission.principal) {
                const res = await this.getUserInfo(permission.name);
                displayName = res.displayName;
                userAttributes = res.userAttributes;
            } else {
                const [role] = (this.roles || [])
                    .filter(r => (r.name || '')
                    .toLowerCase() === (permission.name || '')
                    .toLowerCase());
                if (role && role.name) {
                    displayName = ngbTargetPermissionsFormController.getRoleDisplayName(role);
                } else {
                    displayName = permission.name;
                }
            }
            return { ...permission, displayName, userAttributes };
        };

        const rows = [];
        for (const permission in data) {
            if (data.hasOwnProperty(permission)) {
                rows.push(await mapGridData(data[permission]));
            }
        }
        this.formGridOptions.data = rows;

        if (this.$scope) {
            this.$scope.$apply();
        }
    }

    getUserInfo(userName) {
        return this.ngbTargetPermissionsFormService.getUserInfo(userName).then(userInfo => ({
            displayName: userInfo ? getUserDisplayNameString(userInfo) : userName,
            userAttributes: getUserAttributesString(userInfo) || undefined
        }));
    }

    changeOwner = () => {
        this.ngbTargetPermissionsFormService.grantOwner(this.target, this.selectedOwner)
            .then(() => this.fetchPermissions())
            .then(() => this.clearSelectedOwner())
            .then(() => this.dispatcher.emit('target:owner:changed'));
    };

    clearSelectedOwner = () => {
        this._selectedOwner = null;
    };

    onAddUser() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbTargetAddUserRoleDlgController,
            controllerAs: 'ctrl',
            locals: {
                availableItems: this.availableUsers,
                existedItems: [],
                isUser: true,
                target: this.target
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbTargetAddUserRoleDlg.tpl.html'),
        }).then(() => this.fetchPermissions());
    }

    onAddRole() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbTargetAddUserRoleDlgController,
            controllerAs: 'ctrl',
            locals: {
                availableItems: this.availableRoles,
                existedItems: (this.permissions || []).filter(p => !p.principal).map(p => p.name),
                isUser: false,
                target: this.target
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbTargetAddUserRoleDlg.tpl.html'),
        }).then(() => this.fetchPermissions());
    }

    getSubjectDisplayType() {
        if (this.subject) {
            if (this.subject.principal) {
                return 'user';
            } else {
                const [role] = (this.roles || [])
                    .filter(r => (r.name || '')
                    .toLowerCase() === (this.subject.name || '')
                    .toLowerCase());
                if (role && !role.predefined) {
                    return 'group';
                }
                return 'role';
            }
        }
        return '';
    }

    getSubjectDisplayName() {
        if (this.subject) {
            if (!this.subject.principal) {
                const [role] = (this.roles || [])
                    .filter(r => (r.name || '')
                    .toLowerCase() === (this.subject.name || '')
                    .toLowerCase());
                if (role) {
                    return ngbTargetPermissionsFormController.getRoleDisplayName(role);
                }
            }
            return this.subject.name;
        }
        return '';
    }

    clearOwnerSearchTerm() {
        this.ownerSearchTerm = '';
    }

    // bit:
    // 0 - read allowed
    // 1 - read denied
    // 2 - write allowed
    // 3 - write denied
    changeMask(bit) {
        let {readAllowed, readDenied, writeAllowed, writeDenied} = this._subjectPermissions;
        switch (bit) {
            case 0:
                readAllowed = !readAllowed;
                if (readAllowed) {
                    readDenied = false;
                }
                break;
            case 1:
                readDenied = !readDenied;
                if (readDenied) {
                    readAllowed = false;
                }
                break;
            case 2:
                writeAllowed = !writeAllowed;
                if (writeAllowed) {
                    writeDenied = false;
                }
                break;
            case 3:
                writeDenied = !writeDenied;
                if (writeDenied) {
                    writeAllowed = false;
                }
                break;
        }
        this._subjectPermissions = {
            readAllowed, readDenied, writeAllowed, writeDenied
        };
        this.ngbTargetPermissionsFormService.grantPermission(
            this.target,
            {name: this._subject.name, principal: this._subject.principal},
            roleModel.buildExtendedMask(readAllowed, readDenied, writeAllowed, writeDenied)
        )
            .then(data => {
                if (data && data.permissions && data.permissions.length) {
                    const item = data.permissions[0];
                    const [dataItem] = (this.formGridOptions.data || [])
                        .filter(dI => dI.principal === item.sid.principal && (dI.name || '')
                        .toLowerCase() === (item.sid.name || '')
                        .toLowerCase());
                    if (dataItem) {
                        dataItem.mask = item.mask;
                    }
                }
            });
    }

    deleteSubjectPermissions = (subject, $event) => {
        $event.stopPropagation();
        this.ngbTargetPermissionsFormService.deleteNodePermissions(this.target, subject)
            .then(() => this.fetchPermissions())
            .catch(() => this.fetchPermissions());
    };

    close() {
        this.$mdDialog.hide();
    }
}
