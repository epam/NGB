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
    constructor($mdDialog, $scope, node, ngbPermissionsFormService) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            node,
            ngbPermissionsFormService
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
            }
        });
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
