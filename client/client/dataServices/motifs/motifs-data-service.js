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
                    reject(new Error(error.message || message));
                });
        });
    }

    loadMotifTrack (request) {
        const message = 'Motifs Data Service: error getting motif track';
        return new Promise((resolve, reject) => {
            this.post('reference/motif', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error(message));
                    }
                });
        });
    }
}
