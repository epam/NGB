import {getUserAttributesString} from '../../../../../../shared/components/ngbUserManagement/internal/utilities';

const ACL_CLASS = 'TARGET';

export default class ngbTargetPermissionsFormService {

    get aclClass() {
        return ACL_CLASS;
    }

    static instance(userDataService, roleDataService, permissionsDataService) {
        return new ngbTargetPermissionsFormService(userDataService, roleDataService, permissionsDataService);
    }

    _userDataService;
    _roleDataService;
    _permissionsDataService;

    constructor(userDataService, roleDataService, permissionsDataService) {
        this._userDataService = userDataService;
        this._roleDataService = roleDataService;
        this._permissionsDataService = permissionsDataService;
    }

    getPermissions(target) {
        return Promise.resolve({
            mask: target.mask,
            owner: target.owner,
            permissions: (target.permissions || []).map(p => ({mask: p.mask, ...p.sid}))
        });
        // return this._permissionsDataService.getObjectPermissions(target.id, this.aclClass)
        //     .then(data => {
        //         if (data) {
        //             return {
        //                 mask: data.entity.mask,
        //                 owner: data.entity.owner,
        //                 permissions: (data.permissions || [])
        //                     .map(p => ({mask: p.mask, ...p.sid}))
        //             };
        //         } else {
        //             return {
        //                 error: true,
        //                 message: 'Error fetching object permissions'
        //             };
        //         }
        //     });
    }

    getUsers() {
        return this._userDataService.getUsers()
            .then(users =>
                (users || []).map(u => ({
                    ...u,
                    userAttributes: u.attributes ? getUserAttributesString(u) : undefined
                }))
            );
    }

    getRoles() {
        return this._roleDataService.getRoles();
    }

    grantOwner(target, user) {
        return this._permissionsDataService.grantOwner(target.id, this.aclClass, user);
    }

    deleteNodePermissions(target, userOrGroup) {
        return this._permissionsDataService
            .deleteObjectPermissions(target.id, this.aclClass, userOrGroup.name, userOrGroup.principal);
    }

    grantPermission(target, userOrGroup, mask) {
        return this._permissionsDataService
            .grantPermission(target.id, this.aclClass, userOrGroup.name, userOrGroup.principal, mask);
    }

    searchAdGroups(prefix) {
        return this._roleDataService.findADGroup(prefix);
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
}
