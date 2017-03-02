import {DataService} from '../data-service';
/**
 * data service for genome
 * @extends DataService
 */
export class UtilsDataService extends DataService {

    getFiles(path) {
        return new Promise((resolve) => {
            this.get(path ? `files?path=${path}` : 'files').catch(() => { resolve([]); }).then((data) => {
                resolve(data || {});
            });
        });
    }

    getFilesAllowed() {
        return new Promise((resolve) => {
            this.get('files/allowed').catch(() => { resolve(false); }).then((result) => {
                resolve(result);
            });
        })
    }
}