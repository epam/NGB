import {DataService} from '../data-service';
/**
 * data service for project
 * @extends DataService
 */
export class WigDataService extends DataService {
    /**
     *
     * @returns {promise}
     */
    getWigTrack(wig) {
        if (wig instanceof Array) {
            const promises = [];
            for (let i = 0; i < wig.length; i++) {
                const promise = new Promise((resolve, reject) => {
                    this.post('wig/track/get', wig[i]).then((data)=> {
                        if (data) {
                            resolve(data.blocks ? data.blocks : []);
                        }else {
                            const code = 'WIG Data Service', message = 'WIG Data Service: error loading wig track';
                            reject({code, message});
                        }
                    });
                });
                promises.push(promise);
            }
            return new Promise((resolve) => {
                Promise.all(promises).then(values => {
                    const data = [];
                    for (let i = 0; i < values.length; i++) {
                        for (let j = 0; j < values[i].length; j++) {
                            data.push(values[i][j]);
                        }
                    }
                    resolve(data);
                });
            });
        }
        return new Promise((resolve, reject) => {
            this.post('wig/track/get', wig).then((data)=> {
                if (data) {
                    resolve(data.blocks ? data.blocks : []);
                }else {
                    const code = 'WIG Data Service', message = 'WIG Data Service: error loading wig track';
                    reject({code, message});
                }
            }).catch(() => {
                resolve([]);
            });
        });
    }

    register(referenceId, path ,indexPath, name) {
        return this.post('wig/register', {referenceId, path, indexPath, name});
    }

}
