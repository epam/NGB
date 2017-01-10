import {DataService} from '../data-service';
/**
 * data service for seg type tracks
 * @extends DataService
 */
export class OthersDataService extends DataService {

    getVersion() {
        return new Promise((resolve) => {
            this.get('version').then((version)=> {
                resolve(version);
            });
        });
    }

}