import {DataService} from '../data-service';
/**
 * data service for seg type tracks
 * @extends DataService
 */
export class SegDataService extends DataService {

    loadSegTrack(seg) {
        return new Promise((resolve, reject) => {
            this.post('seg/track/get', seg).then((data)=> {
                if (data) {
                    resolve(data.tracks ? data.tracks : []);
                } else {
                    const code = 'Genome Data Service', message = 'Genome Data Service: error loading seg track';
                    reject({code, message});
                }
            });
        });
    }

    register(referenceId, path ,indexPath, name) {
        return this.post('seg/register', {referenceId, path, indexPath, name});
    }
}
