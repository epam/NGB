export default class ngbPermissionsFormService {

    static instance(userDataService, roleDataService, permissionsDataService) {
        return new ngbPermissionsFormService(userDataService, roleDataService, permissionsDataService);
    }

    _userDataService;
    _roleDataService;
    _permissionsDataService;

    constructor(userDataService, roleDataService, permissionsDataService) {
        this._userDataService = userDataService;
        this._roleDataService = roleDataService;
        this._permissionsDataService = permissionsDataService;
    }

    static getNodeAclClass(node) {
        if (node.isProject) {
            return 'PROJECT';
        } else {
            return (node.format || '').toUpperCase();
        }
    }

    getUsers() {
        return this._userDataService.getUsers();
    }

    getRoles() {
        return this._roleDataService.getRoles();
    }

    getNodePermissions(node) {
        const aclClass = ngbPermissionsFormService.getNodeAclClass(node);
        const mapPermission = p => ({mask: p.mask, ...p.sid});
        return this._permissionsDataService
            .getObjectPermissions(node.id, aclClass)
            .then(data => {
                if (data) {
                    return {
                        mask: data.entity.mask,
                        owner: data.entity.owner,
                        permissions: (data.permissions || []).map(mapPermission)
                    };
                } else {
                    return {
                        error: true,
                        message: 'Error fetching object permissions'
                    };
                }
            });
    }

    deleteNodePermissions(node, userOrGroup) {
        const aclClass = ngbPermissionsFormService.getNodeAclClass(node);
        return this._permissionsDataService
            .deleteObjectPermissions(node.id, aclClass, userOrGroup.name, userOrGroup.principal);
    }

    grantPermission(node, userOrGroup, mask) {
        const aclClass = ngbPermissionsFormService.getNodeAclClass(node);
        return this._permissionsDataService
            .grantPermission(node.id, aclClass, userOrGroup.name, userOrGroup.principal, mask);
    }

    grantOwner(node, user) {
        const aclClass = ngbPermissionsFormService.getNodeAclClass(node);
        return this._permissionsDataService
            .grantOwner(node.id, aclClass, user);
    }
}