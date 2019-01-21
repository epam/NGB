import {DataService} from '../data-service';

/**
 * data service for users
 * @extends DataService
 */
export class UserDataService extends DataService {

    getUsers() {
        return new Promise(resolve => {
            this.get('users')
                .then((data) => {
                    this.cachedUsers = data;
                    resolve(data || null);
                })
                .catch(() => {
                    resolve(null);
                });
        });
    }

    cachedUsers = [];

    getCachedUsers() {
        if (this.cachedUsers && this.cachedUsers.length > 0) {
            return new Promise(resolve => resolve(this.cachedUsers));
        } else {
            return this.getUsers().then(users => {
                this.cachedUsers = users;
                return users;
            });
        }
    }

    getUser(id) {
        return new Promise(resolve => {
            this.get(`user/${id}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch(() => {
                    resolve(null);
                });
        });
    }

    _currentUser;

    getCurrentUser() {
        return new Promise(resolve => {
            if (this._currentUser) {
                resolve(this._currentUser);
            } else {
                this.get('user/current')
                    .then((data) => {
                        this._currentUser = data;
                        this._currentUser.hasRole = function (roleName) {
                            return (this.roles || [])
                                .filter(r => (r.name || '').toUpperCase() === (roleName || '').toUpperCase())
                                .length > 0;
                        };
                        this._currentUser.hasRoles = function (roleNames) {
                            for (let i = 0; i < (roleNames || []).length; i++) {
                                if (this.hasRole(roleNames[i])) {
                                    return true;
                                }
                            }
                            return (roleNames || []).length === 0;
                        };
                        resolve(this._currentUser);
                    })
                    .catch(() => {
                        resolve(null);
                    });
            }
        });
    }

    currentUserIsAdmin() {
        return new Promise(resolve => {
            this.getCurrentUser()
                .then((user) => {
                    if (user) {
                        resolve(user.enabled && user.roles && user.roles.filter(r => r.name === 'ROLE_ADMIN').length > 0);
                    } else {
                        resolve(false);
                    }
                });
        });
    }

    getJwtToken(expiration = null) {
        return new Promise(resolve => {
            this.get(expiration ? `user/token?expiration=${expiration}` : 'user/token')
                .then((data) => {
                    resolve(data.token || null);
                })
                .catch(() => {
                    resolve(null);
                });
        });
    }

    createUser(userBody) {
        return new Promise((resolve, reject) => {
            this.post('user', userBody)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    reject(error.message || error);
                });
        });
    }

    updateUser(id, userBody) {
        return new Promise((resolve, reject) => {
            this.put(`user/${id}`, userBody)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    reject(error.message || error);
                });
        });
    }

    deleteUser(id) {
        return new Promise((resolve, reject) => {
            this.delete(`user/${id}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    reject(error.message || error);
                });
        });
    }

}
