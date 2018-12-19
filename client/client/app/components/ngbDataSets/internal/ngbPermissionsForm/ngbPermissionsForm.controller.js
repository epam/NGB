import BaseController from '../../../../shared/baseController';
import roleModel from '../../../../shared/utils/roleModel';

const ROLE_NAME_FIRST_PART = 'ROLE_';

export default class ngbPermissionsFormController extends BaseController {

    node;
    ngbPermissionsFormService;
    owner;
    permissions;
    users;
    roles;

    ownerSearchTerm;
    formGridOptions = {};

    selectedPermissionSubject;
    selectedPermissionSubjectPermissions = {
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
            // columnDefs: this.ngbPermissionsFormService.getPermissionsColumns(),
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
            console.log(this.availableRoles);
        });
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

    getRoleDisplayName(role) {
        if (role.predefined && role.name.toUpperCase().startsWith(ROLE_NAME_FIRST_PART)) {
            return role.name.substring(ROLE_NAME_FIRST_PART.length);
        }
        return role.name;
    }

    fetchPermissions() {
        this.ngbPermissionsFormService.getNodePermissions(this.node).then(data => {
            if (data) {
                this.owner = data.owner;
                this.permissions = data.permissions;
                if (this.$scope) {
                    this.$scope.$apply();
                }
                console.log('permissions', data.owner, data.permissions);
            }
        });
    }

    clearOwnerSearchTerm() {
        this.ownerSearchTerm = '';
    }

    selectPermissionSubject = (subject) => {
        this.selectedPermissionSubject = subject; // {name, principal, mask};
        this.selectedPermissionSubjectPermissions = {
            readAllowed: roleModel.readAllowed(subject, true),
            readDenied: roleModel.readDenied(subject, true),
            writeAllowed: roleModel.writeAllowed(subject, true),
            writeDenied: roleModel.writeDenied(subject, true)
        };
    };

    // value: true/false (checkbox)
    // bit:
    // 0 - read allowed
    // 1 - read denied
    // 2 - write allowed
    // 3 - write denied
    changeMask(value, bit) {
        let {readAllowed, readDenied, writeAllowed, writeDenied} = this.selectedPermissionSubjectPermissions;
        switch (bit) {
            case 0:
                readAllowed = value;
                if (value) {
                    readDenied = false;
                }
                break;
            case 1:
                readDenied = value;
                if (value) {
                    readAllowed = false;
                }
                break;
            case 2:
                writeAllowed = value;
                if (value) {
                    writeDenied = false;
                }
                break;
            case 3:
                writeDenied = value;
                if (value) {
                    writeAllowed = false;
                }
                break;
        }
        this.ngbPermissionsFormService
            .grantPermission(
                this.node,
                this.selectedPermissionSubject,
                roleModel.buildExtendedMask(readAllowed, readDenied, writeAllowed, writeDenied)
            ).then(() => {
            this.selectedPermissionSubjectPermissions = {
                readAllowed, readDenied, writeAllowed, writeDenied
            };
        });
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

    changeOwner = (newOwner) => {
        this.ngbPermissionsFormService
            .grantOwner(this.node, newOwner)
            .then(this.fetchPermissions);
    };

    close() {
        this.$mdDialog.hide();
    }

}
