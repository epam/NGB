import {DataService} from '../data-service';
/**
 * data service for motifs type tracks
 * @extends DataService
 */
export class MotifsDataService extends DataService {

    getSearchMotifsResults (request) {
        const message = 'Motifs Data Service: error getting search motif results';
        return new Promise((resolve, reject) => {
            this.post('reference/motif/table', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error(message));
                    }
                })
                .catch(error => {
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    loadMotifTrack (request) {
        return new Promise((resolve, reject) => {
            this.post('reference/motif', request)
                .catch(() => {
                    resolve({});
                })
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        const code = 'Motifs Data Service';
                        const message = 'Motifs Data Service: error loading motif track';
                        reject({code, message});
                    }
                });
        });
    }

    getNextMotifs (request) {
        return new Promise((resolve) => {
            this.post('reference/motif/next', request)
                .catch(() => {
                    resolve([]);
                })
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve({});
                    }
                });
        });
    }

    getPrevMotifs (request) {
        return new Promise((resolve) => {
            this.post('reference/motif/prev', request)
                .catch(() => {
                    resolve([]);
                })
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve({});
                    }
                });
        });
    }
}
