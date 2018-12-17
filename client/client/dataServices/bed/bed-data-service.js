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
        let url = 'bed/track/get';
        if (bed.openByUrl) {
            url = `bed/track/get?fileUrl=${encodeURIComponent(bed.fileUrl)}&indexUrl=${encodeURIComponent(bed.indexUrl)}`;
            bed.openByUrl = undefined;
            bed.fileUrl = undefined;
            bed.indexUrl = undefined;
            bed.projectId = undefined;
        }
        return new Promise((resolve, reject) => {
            this.post(url, bed).then((data)=> {
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
        if (bed.openByUrl) {
            return new Promise((resolve) => { resolve([]); });
        }
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

    register(referenceId, path ,indexPath, name) {
        return this.post('bed/register', {referenceId, path, indexPath, name});
    }

}
