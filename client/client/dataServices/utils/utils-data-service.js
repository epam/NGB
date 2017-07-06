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
        })
    }

    getDefaultTrackSettings() {
        return new Promise((resolve) => {
            this.get('defaultTrackSettings').catch(() => {
                resolve(false);
            }).then((result) => {
                resolve(result);
            });
        })
    }

    generateShortUrl(fullUrl, alias) {
        return new Promise((resolve) => {
            this.post('generateShortUrl', {
                url: fullUrl,
                alias: (alias && alias.length) ? alias : undefined
            }).catch(() => {
                resolve('');
            }).then((result) => {
                const shortUrl = this._serverUrl.endsWith('/') ?
                    `${this._serverUrl}${result}` :
                    `${this._serverUrl}/${result}`;
                resolve(shortUrl);
            });
        })
    }
}