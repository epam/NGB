import {DataService} from '../data-service';
/**
 * data service for project
 * @extends DataService
 */
export class BedDataService extends DataService {
    /**
     *
     * @returns {promise}
     */
    getBedTrack(bed) {
        return new Promise((resolve, reject) => {
            this.post('bed/track/get', bed).then((data)=> {
                if (data) {
                    resolve(data.blocks ? data.blocks : []);
                }else {
                    const code = 'BED Data Service', message = 'BED Data Service: error loading bed track';
                    reject({code, message});
                }
            });


        });
    }
    getBedHistogram(bed){
        return new Promise((resolve, reject) => {
            this.post('bed/track/histogram', bed).then((data)=> {
                if (data) {
                    resolve(data.blocks ? data.blocks : []);
                }else {
                    const code = 'BED Data Service', message = 'BED Data Service: error loading bed histogram';
                    reject({code, message});
                }
            });
        });
    }

    getAllFileList(refId) {
        return this.get(`bed/${refId}/loadAll`);
    }

    register(referenceId, path ,indexPath, name) {
        return this.post('bed/register', {referenceId, path, indexPath, name});
    }

}