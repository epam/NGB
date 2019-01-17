import {DataService} from '../data-service';
/**
 * data service for roles
 * @extends DataService
 */
export class RoleDataService extends DataService {

    getRoles(loadUsers = false) {
        return new Promise(resolve => {
            this.get(loadUsers ? 'role/loadAll?loadUsers=true' : 'role/loadAll')
                .then((data) => {
                    resolve(data || null);
                })
                .catch(() => {
                    resolve(null);
                });
        });
    }

    getRole(id) {
        return new Promise(resolve => {
            this.get(`role/${id}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch(() => {
                    resolve(null);
                });
        });
    }

    createGroup(name, userDefault = false) {
        return new Promise((resolve, reject) => {
            this.post(`role/create?roleName=${name}&userDefault=${userDefault}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    reject(error.message || error);
                });
        });
    }

    assignUsersToRole(roleId, usersIds = []) {
        return new Promise((resolve, reject) => {
            this.post(`role/${roleId}/assign?userIds=${usersIds}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    reject(error.message || error);
                });
        });
    }

    updateRole(id, roleBody) {
        return new Promise((resolve, reject) => {
            this.put(`role/${id}`, roleBody)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    reject(error.message || error);
                });
        });
    }

    deleteRole(id) {
        return new Promise((resolve, reject) => {
            this.delete(`role/${id}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    reject(error.message || error);
                });
        });
    }

    removeRoleFromUsers(roleId, usersIds = []) {
        return new Promise((resolve, reject) => {
            this.delete(`role/${roleId}/remove?userIds=${usersIds}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    reject(error.message || error);
                });
        });
    }

    findADGroup(prefix) {
        return new Promise(resolve => {
            this.get(prefix ? `group/find?prefix=${encodeURIComponent(prefix)}` : 'group/find')
                .then((data) => {
                    resolve(data || []);
                })
                .catch(() => {
                    resolve([]);
                });
        });
    }

}
