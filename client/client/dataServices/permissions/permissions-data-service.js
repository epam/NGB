import {DataService} from '../data-service';

/**
 * data service for permissions
 * @extends DataService
 */
export class PermissionsDataService extends DataService {

    getObjectPermissions(id, aclClass) {
        return new Promise(resolve => {
            this.get(`grant?id=${id}&aclClass=${aclClass}`)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    deleteObjectPermissions(id, aclClass, name, isPrincipal) {
        return new Promise(resolve => {
            this.delete(`grant?id=${id}&aclClass=${aclClass}&user=${name}&isPrincipal=${isPrincipal}`)
                .catch(() => resolve(null))
                .then(data => {
                    resolve(data || null);
                });
        });
    }

    grantPermission(id, aclClass, name, isPrincipal, mask) {
        const body = {
            aclClass,
            id,
            mask,
            principal: isPrincipal,
            userName: name
        };
        return new Promise(resolve => {
            this.post('grant', body)
                .catch(() => resolve(null))
                .then(data => {
                    resolve(data || null);
                });
        });
    }

    grantOwner(id, aclClass, owner) {
        return new Promise(resolve => {
            this.post(`grant/owner?id=${id}&aclClass=${aclClass}&userName=${owner}`)
                .catch(() => resolve(null))
                .then(data => {
                    resolve(data || null);
                });
        });
    }

}
