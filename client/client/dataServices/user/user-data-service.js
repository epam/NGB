import {DataService} from '../data-service';
/**
 * data service for users
 * @extends DataService
 */
export class UserDataService extends DataService {

    getUsers() {
        return new Promise(resolve => {
            this.get('users')
                .catch(() => {
                    debugger;
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    getUser(id) {
        return new Promise(resolve => {
            this.get(`user/${id}`)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    getCurrentUser() {
        return new Promise(resolve => {
            this.get('user/current')
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    getJwtToken(expiration = null) {
        return new Promise(resolve => {
            this.get(expiration ? `user/token?expiration=${expiration}` : 'user/token')
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data.token || null);
                });
        });
    }

    createUser(userBody) {
        return new Promise(resolve => {
            this.post('user', userBody)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    updateUser(id, userBody) {
        return new Promise(resolve => {
            this.put(`user/${id}`, userBody)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

    deleteUser(id) {
        return new Promise(resolve => {
            this.delete(`user/${id}`)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    resolve(data || null);
                });
        });
    }

}
