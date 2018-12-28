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

    isRoleModelEnabled() {
        return new Promise((resolve) => {
            this.get('isRoleModelEnabled').then((result) => {
                resolve(`${result}`.toLowerCase() === 'true');
            }).catch(() => {
                resolve(false);
            });
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
