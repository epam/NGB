import angular from 'angular';
import BaseController from '../../../../shared/baseController';
import roleModel from '../../../../shared/utils/roleModel';
import ngbAddUserRoleDlgController from './ngbAddUserRoleDlg.controller';

const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbPermissionsFormController extends BaseController {

    node;
    ngbPermissionsFormService;
    owner;
    mask;
    _selectedOwner;
    permissions = [];
    users;
    roles;

    ownerSearchTerm;
    formGridOptions = {};

    _subject = null;
    _subjectPermissions = {
        readAllowed: false,
        readDenied: false,
        writeAllowed: false,
        writeDenied: false
    };

    static get UID() {
        return 'ngbPermissionsFormController';
    }

    /* @ngInject */
    constructor($mdDialog, $scope, node, ngbPermissionsFormService, ngbPermissionsGridOptionsConstant) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            node,
            ngbPermissionsFormService
        });

        Object.assign(this.formGridOptions, {
            ...ngbPermissionsGridOptionsConstant,
            appScopeProvider: this.scope,
            columnDefs: this.ngbPermissionsFormService.getPermissionsColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, (row) => this.selectPermissionSubject(row));
            },
            // showHeader: false,
        });

        this.fetchPermissions();
        this.ngbPermissionsFormService.getUsers().then(users => {
            this.users = users || [];
        });
        this.ngbPermissionsFormService.getRoles().then(roles => {
            this.roles = roles || [];
            if (this.permissions) {
                this.setPermissionsGridData(this.permissions);
            }
        });
    }

    get selectedOwner() {
        if (this._selectedOwner) {
            return this._selectedOwner;
        }
        return this.owner;
    }

    get ownerChangeAllowed() {
        return roleModel.isOwner({mask: this.mask});
    }

    get permissionsChangeAllowed() {
        return roleModel.writeAllowed({mask: this.mask});
    }

    set selectedOwner(value) {
        this._selectedOwner = value;
    }

    get ownerChanged() {
        return this._selectedOwner && this._selectedOwner !== this.owner;
    }

    setPermissionsGridData(data) {
        if (this.subject && !data.map(i => i.name).includes(this.subject.name)) {
            this._subject = null;
        }
        const mapGridData = (permission) => {
            let displayName;
            if(permission.principal) {
                displayName = permission.name;
            } else {
                const [role] = (this.roles || []).filter(r => (r.name || '').toLowerCase() === (permission.name || '').toLowerCase());
                if (role && role.name) {
                    displayName = ngbPermissionsFormController.getRoleDisplayName(role);
                } else {
                    displayName = permission.name;
                }
            }
            return { ...permission, displayName };
        };
        this.formGridOptions.data = data.map(mapGridData);
        if (this.$scope) {
            this.$scope.$apply();
        }
    }

    get availableUsers() {
        return (this.users || [])
            .filter(u => (this.permissions || [])
                .filter(p => p.principal && (p.name || '').toLowerCase() === (u.userName || '').toLowerCase()).length === 0);
    }

    get availableRoles() {
        return (this.roles || [])
            .filter(r => (this.permissions || [])
                .filter(p => !p.principal && (p.name || '').toLowerCase() === (r.name || '').toLowerCase()).length === 0);
    }

    get subject() {
        return this._subject;
    }

    get subjectPermissions() {
        return this._subjectPermissions;
    }

    static getRoleDisplayName(role) {
        if (!role.predefined && role.name.toUpperCase().startsWith(ROLE_NAME_FIRST_PART)) {
            return role.name.substring(ROLE_NAME_FIRST_PART.length);
        }
        return role.name;
    }

    getSubjectDisplayType() {
        if (this.subject) {
            if (this.subject.principal) {
                return 'user';
            } else {
                const [role] = (this.roles || []).filter(r => (r.name || '').toLowerCase() === (this.subject.name || '').toLowerCase());
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
                const [role] = (this.roles || []).filter(r => (r.name || '').toLowerCase() === (this.subject.name || '').toLowerCase());
                if (role) {
                    return ngbPermissionsFormController.getRoleDisplayName(role);
                }
            }
            return this.subject.name;
        }
        return '';
    }

    fetchPermissions() {
        this.ngbPermissionsFormService.getNodePermissions(this.node).then(data => {
            if (data) {
                this.owner = (data.owner || '').toUpperCase();
                this.mask = data.mask;
                this.permissions = data.permissions;
                this.setPermissionsGridData(this.permissions);
            }
        });
    }

    clearOwnerSearchTerm() {
        this.ownerSearchTerm = '';
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
        this.ngbPermissionsFormService
            .grantPermission(
                this.node,
                {name: this._subject.name, principal: this._subject.principal},
                roleModel.buildExtendedMask(readAllowed, readDenied, writeAllowed, writeDenied)
            )
            .then(data => {
                if (data && data.permissions && data.permissions.length) {
                    const item = data.permissions[0];
                    const [dataItem] = (this.formGridOptions.data || [])
                        .filter(dI => dI.principal === item.sid.principal && (dI.name || '').toLowerCase() === (item.sid.name || '').toLowerCase());
                    if (dataItem) {
                        dataItem.mask = item.mask;
                    }
                }
            });
    }

    onAddRole() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbAddUserRoleDlgController,
            controllerAs: 'ctrl',
            locals: {
                availableItems: this.availableRoles,
                existedItems: (this.permissions || []).filter(p => !p.principal).map(p => p.name),
                isUser: false,
                node: this.node
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbAddUserRoleDlg.tpl.html'),
        }).then(() => this.fetchPermissions());
    }

    onAddUser() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbAddUserRoleDlgController,
            controllerAs: 'ctrl',
            locals: {
                availableItems: this.availableUsers,
                existedItems: [],
                isUser: true,
                node: this.node
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('./ngbAddUserRoleDlg.tpl.html'),
        }).then(() => this.fetchPermissions());
    }

    deleteSubjectPermissions = (subject) => {
        this.ngbPermissionsFormService
            .deleteNodePermissions(this.node, subject)
            .then(() => this.fetchPermissions())
            .catch(() => this.fetchPermissions());
    };

    clearSelectedOwner = () => {
        this._selectedOwner = null;
    };

    changeOwner = () => {
        this.ngbPermissionsFormService
            .grantOwner(this.node, this.selectedOwner)
            .then(() => this.fetchPermissions())
            .then(() => this.clearSelectedOwner());
    };

    close() {
        this.$mdDialog.hide();
    }

}
