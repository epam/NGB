import BaseController from '../../../../shared/baseController';
import roleModel from '../../../../shared/utils/roleModel';

const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbPermissionsFormController extends BaseController {

    node;
    ngbPermissionsFormService;
    owner;
    permissions = [];
    users;
    roles;

    ownerSearchTerm;
    formGridOptions = {};

    _subject;
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
            },
            // showHeader: false,
        });

        this.fetchPermissions();
        this.ngbPermissionsFormService.getUsers().then(users => {
            this.users = users || [];
        });
        this.ngbPermissionsFormService.getRoles().then(roles => {
            this.roles = roles || [];
        });
    }

    setPermissionsGridData(data) {
        this.formGridOptions.data = data;
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

    getRoleDisplayName(role) {
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
                const [role] = this.roles.filter(r => (r.name || '').toLowerCase() === (this.subject.name || '').toLowerCase());
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
                const [role] = this.roles.filter(r => (r.name || '').toLowerCase() === (this.subject.name || '').toLowerCase());
                if (role) {
                    return this.getRoleDisplayName(role);
                }
            }
            return this.subject.name;
        }
        return '';
    }

    fetchPermissions() {
        this.ngbPermissionsFormService.getNodePermissions(this.node).then(data => {
            if (data) {
                this.owner = data.owner;
                this.permissions = data.permissions;
                this.setPermissionsGridData(this.permissions);
                if (this.$scope) {
                    this.$scope.$apply();
                }
            }
        });
    }

    clearOwnerSearchTerm() {
        this.ownerSearchTerm = '';
    }

    selectPermissionSubject = (subject) => {
        this._subject = subject; // {name, principal, mask};
        this._subjectPermissions = {
            readAllowed: roleModel.readAllowed(subject, true),
            readDenied: roleModel.readDenied(subject, true),
            writeAllowed: roleModel.writeAllowed(subject, true),
            writeDenied: roleModel.writeDenied(subject, true)
        };
        this.$scope.$apply();
    };

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
                this._subject,
                roleModel.buildExtendedMask(readAllowed, readDenied, writeAllowed, writeDenied)
            );
    }

    onAddRole() {
        console.log('open add role/group dialog');
        // todo show add dialog
        // this.$mdDialog.show().then(result => {
            // todo add group-role/fetch
        // });
    }

    onAddUser() {
        console.log('open add user dialog');
        // todo show add dialog
        // this.$mdDialog.show().then(result => {
            // todo add user/fetch
        // });
    }

    deleteSubjectPermissions = (subject) => {
        this.ngbPermissionsFormService
            .deleteNodePermissions(this.node, subject)
            .then(this.fetchPermissions);
    };

    onOwnerChange($event) {
        console.log(this.owner);
    }

    changeOwner = (newOwner) => {
        this.ngbPermissionsFormService
            .grantOwner(this.node, newOwner)
            .then(this.fetchPermissions);
    };

    close() {
        this.$mdDialog.hide();
    }

}
