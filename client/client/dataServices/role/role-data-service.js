import {DataService} from '../data-service';
/**
 * data service for roles
 * @extends DataService
 */
export class RoleDataService extends DataService {

    getRoles(loadUsers = false) {
        return new Promise(resolve => {
            this.get(loadUsers ? 'role/loadAll?loadUsers=true' : 'role/loadAll')
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    getRole(id) {
        return new Promise(resolve => {
            this.get(`role/${id}`)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    createGroup(name, userDefault = false) {
        return new Promise(resolve => {
            this.post(`role/create?roleName=${name}&userDefault=${userDefault}`)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    assignUsersToRole(roleId, usersIds = []) {
        return new Promise(resolve => {
            this.post(`role/${roleId}/assign?userIds=${usersIds}`)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    updateRole(id, roleBody) {
        return new Promise(resolve => {
            this.put(`role/${id}`, roleBody)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    deleteRole(id) {
        return new Promise(resolve => {
            this.delete(`role/${id}`)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    removeRoleFromUsers(roleId, usersIds = []) {
        return new Promise(resolve => {
            this.delete(`role/${roleId}/remove?userIds=${usersIds}`)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

}
