import {DataService} from '../data-service';
/**
 * data service for maf type tracks
 * @extends DataService
 */
export class MafDataService extends DataService {

    loadMafTrack(maf) {
        return new Promise((resolve, reject) => {
            this.post('maf/track/get', maf).then((data)=> {
                if (data) {
                    resolve(data.blocks ? data.blocks : []);
                } else {
                    const code = 'Genome Data Service', message = 'Genome Data Service: error loading maf track';
                    reject({code, message});
                }
            });
        });
    }

    register(referenceId, path, indexPath, name) {
        return this.post('maf/register', {referenceId, path, indexPath, name});
    }
}
