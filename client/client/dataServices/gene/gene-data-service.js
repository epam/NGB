import {DataService} from '../data-service';
/**
 * data service for gene
 * @extends DataService
 */
export class GeneDataService extends DataService {


    register(referenceId, path, indexPath, name) {
        return this.post('gene/register', {referenceId, path, indexPath, name});
    }

    loadGeneHistogramTrack(gene) {
        if (gene.openByUrl) {
            return new Promise((resolve) => { resolve([]); });
        }
        return new Promise((resolve, reject) => {
            this.post('gene/track/histogram', gene).then((data)=> {
                if (data) {
                    resolve(data.blocks ? data.blocks : []);
                } else {
                    const code = 'Gene Data Service', message = 'Gene Data Service: error loading gene histogram track';
                    reject({code, message});
                }
            });
        });
    }

    loadGeneTrack(gene, referenceId) {
        let url = `gene/${referenceId}/track/get`;
        if (gene.openByUrl) {
            url = `gene/${referenceId}/track/get?fileUrl=${encodeURIComponent(gene.fileUrl)}&indexUrl=${encodeURIComponent(gene.indexUrl)}`;
            gene.openByUrl = undefined;
            gene.fileUrl = undefined;
            gene.indexUrl = undefined;
            gene.id = 1;
        }
        return new Promise((resolve, reject) => {
            this.post(url, gene).catch(() => { resolve([]); }).then((data)=> {
                if (data) {
                    resolve(data.blocks ? data.blocks : []);
                } else {
                    const code = 'Gene Data Service', message = 'Gene Data Service: error loading gene track';
                    reject({code, message});
                }
            });
        });
    }

    loadGeneTranscriptTrackPromise = {};
    loadGeneTranscriptTrackStorage = {};

    loadGeneTranscriptTrack(gene) {
        const cacheName = (()=> {
            const arr = ['loadGeneTranscriptTrack', ...Object.values(gene)];
            return arr.join('_');
        })();

        const cache = this.loadGeneTranscriptTrackStorage[cacheName];
        if (cache) {
            return new Promise((resolve) => {
                setTimeout(()=> {
                    resolve(JSON.parse(cache));
                }, 0);
            });
        }

        if (this.loadGeneTranscriptTrackPromise[cacheName]) {
            return this.loadGeneTranscriptTrackPromise[cacheName];
        }

        const errorHandler = (()=> {
            const code = 'Gene Data Service', message = 'Gene Data Service: error loading gene transcript track';
            return {code, message};
        })();

        this.loadGeneTranscriptTrackPromise[cacheName] = new Promise((resolve, reject) => {
            this.post('gene/transcript/track/get', gene).catch(() => { resolve([]); }).then((data)=> {
                if (data) {
                    const resolvedData = data.blocks ? data.blocks : [];
                    resolve(resolvedData);
                    this.loadGeneTranscriptTrackStorage[cacheName] = JSON.stringify(resolvedData);
                } else {
                    reject(errorHandler);
                }
                this.loadGeneTranscriptTrackPromise[cacheName] = null;
                return data;
            }, ()=> {
                reject(errorHandler);
            });
        });

        return this.loadGeneTranscriptTrackPromise[cacheName];
    }

    getPdbDescription(pbdID) {
        const errorHandler = (()=> {
            const code = 'Gene Data Service', message = 'Gene Data Service: error loading pdb description';
            return {code, message};
        })();

        return new Promise((resolve, reject) => {
            this.post(`gene/pbd/${pbdID}/get`, pbdID).then((data)=> {
                if (data) {
                    resolve(data);
                } else {
                    reject(errorHandler);
                }
            }, ()=> {
                reject(errorHandler);
            });
        });
    }

    getAllFileList(refId) {
        return this.get(`gene/${refId}/loadAll`);
    }

    getExonsByViewport(request) {
        const errorHandler = (()=> {
            const code = 'Gene Data Service', message = 'Gene Data Service: error loading exons map';
            return {code, message};
        })();
        return new Promise((resolve, reject) => {
            this.post('gene/exons/viewport', request).then((data) => {
                if (data) {
                    resolve(data);
                }
                else {
                    reject(errorHandler);
                }
            }, () => {
                reject(errorHandler);
            });
        });
    }

    getExonsByRange(request) {
        const errorHandler = (()=> {
            const code = 'Gene Data Service', message = 'Gene Data Service: error loading exons map';
            return {code, message};
        })();
        return new Promise((resolve, reject) => {
            this.post('gene/exons/range', request).then((data) => {
                if (data) {
                    resolve(data);
                }
                else {
                    reject(errorHandler);
                }
            }, () => {
                reject(errorHandler);
            });
        });
    }

}
