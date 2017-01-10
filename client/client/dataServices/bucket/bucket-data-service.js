import {DataService} from '../data-service';
/**
 * data service for bookmarks
 * @extends DataService
 */
export class BucketDataService extends DataService {

    loadAllBuckets() {
        return new Promise((resolve, reject) => {
            this.get('bucket/loadAll').then((data)=> {
                if (data) {
                    resolve(data);
                } else {
                    const code = 'Bucket Data Service', message = 'Bucket Data Service: error loading buckets';
                    reject({code, message});
                }
            });
        });
    }

    loadBucket(bucketId) {
        return new Promise((resolve, reject) => {
            this.get(`bucket/${bucketId}/load`).then((data)=> {
                if (data) {
                    resolve(data);
                } else {
                    const code = 'Bucket Data Service', message = 'Bucket Data Service: error loading bucket';
                    reject({code, message});
                }
            });
        });
    }

    saveBucket(bucket) {
        return new Promise((resolve, reject) => {
            this.post('bucket/save', bucket).then((data)=> {
                if (data) {
                    resolve(data);
                } else {
                    const code = 'Bucket Data Service', message = 'Bucket Data Service: error saving bucket';
                    reject({code, message});
                }
            });
        });
    }
}