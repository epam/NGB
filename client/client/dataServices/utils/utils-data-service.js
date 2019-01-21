import {
    SessionExpirationBehavior,
    SessionExpirationBehaviorStorageKey
} from './session-expiration-behavior';
import {DataService} from '../data-service';

/**
 * data service for genome
 * @extends DataService
 */
export class UtilsDataService extends DataService {

    getFiles(path) {
        return new Promise((resolve) => {
            this.get(path ? `files?path=${path}` : 'files').catch(() => {
                resolve([]);
            }).then((data) => {
                resolve(data || {});
            });
        });
    }

    getFilesAllowed() {
        return new Promise((resolve) => {
            this.get('files/allowed').catch(() => {
                resolve(false);
            }).then((result) => {
                resolve(result);
            });
        });
    }

    getDefaultTrackSettings() {
        return new Promise((resolve) => {
            this.get('defaultTrackSettings').catch(() => {
                resolve(false);
            }).then((result) => {
                resolve(result);
            });
        });
    }

    _isRoleModelEnabled;

    isRoleModelEnabled() {
        return new Promise((resolve) => {
            if (this._isRoleModelEnabled !== null && this._isRoleModelEnabled !== undefined) {
                resolve(this._isRoleModelEnabled);
            } else {
                this.get('isRoleModelEnabled').then((result) => {
                    this._isRoleModelEnabled = `${result}`.toLowerCase() === 'true';
                    resolve(this._isRoleModelEnabled);
                }).catch(() => {
                    resolve(false);
                });
            }
        });
    }

    checkSessionExpirationBehavior() {
        return new Promise((resolve) => {
            this.get('sessionExpirationBehavior')
                .then((result) => {
                    if (result) {
                        localStorage.setItem(SessionExpirationBehaviorStorageKey, result);
                        resolve(result);
                    } else {
                        resolve(SessionExpirationBehavior.confirm);
                    }
                })
                .catch(() => resolve(SessionExpirationBehavior.confirm));
        });
    }

    generateShortUrl(fullUrl, alias) {
        return new Promise((resolve) => {
            this.post('generateShortUrl', {
                url: fullUrl,
                alias: (alias && alias.length) ? alias : undefined
            }).catch(() => {
                resolve('');
            }).then((result) => {
                const a = window.document.createElement('a');
                const baseUrl = this._serverUrl;
                a.href = baseUrl.endsWith('/') ?
                    `${baseUrl}navigate?alias=${result}` :
                    `${baseUrl}/navigate?alias=${result}`;
                resolve(a.href);
            });
        });
    }
}
