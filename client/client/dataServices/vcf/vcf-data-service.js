import {DataService} from '../data-service';
/**
 * data service for project
 * @extends DataService
 */
export class VcfDataService extends DataService {
    /**
     *
     * @returns {promise}
     */
    getVcfFieldInfo(vcfFieldId) {
        return this.get(`vcf/${vcfFieldId}/fieldInfo`);
    }

    register(referenceId, path, indexPath, name) {
        return this.post('vcf/register', {referenceId, path, indexPath, name});
    }

    getVariantInfo(variantData) {
        return new Promise((resolve) => {
            this.post('vcf/variation/load', variantData)
                .catch(() => {
                    resolve(null);
                })
                .then((data) => {
                    if (data) {
                        resolve(data);
                    }
                    else {
                        resolve(null);
                    }
                });
        });
    }

    loadVcfTrack(vcf) {
        return new Promise((resolve, reject) => {
            this.post('vcf/track/get', vcf).then((data) => {
                if (data) {
                    resolve(data.blocks ? data.blocks : []);
                } else {
                    const code = 'Vcf Data Service', message = 'Vcf Data Service: error loading vcf track';
                    reject({code, message});
                }
            });
        });
    }

    getAllFileList(refId) {
        return this.get(`vcf/${refId}/loadAll`);
    }

    getNextVariations(tracksVCF, chromosomeId, fromPosition) {
        if (tracksVCF instanceof Array) {
            const promises = [];
            tracksVCF.forEach(trackVCF => {
                const promise = new Promise((resolve) => {
                    this.get(`vcf/${trackVCF.id}/${chromosomeId}/next/?fromPosition=${fromPosition}`).then((data) => {
                        if (data) {
                            resolve(data);
                        }
                    });
                });
                promises.push(promise);
            });
            return new Promise((resolve) => {
                Promise.all(promises).then(values => {
                    resolve(values);
                });
            });
        }
        return new Promise((resolve, reject) => {
            this.get(`vcf/${tracksVCF.id}/${chromosomeId}/next/?fromPosition=${fromPosition}`).catch(() => {
                resolve([]);
            }).then((data) => {
                if (data) {
                    resolve(data);
                }
            });
        });
    }

    getPreviousVariations(tracksVCF, chromosomeId, fromPosition) {
        if (tracksVCF instanceof Array) {
            const promises = [];
            tracksVCF.forEach(trackVCF => {
                const promise = new Promise((resolve) => {
                    this.get(`vcf/${trackVCF.id}/${chromosomeId}/prev/?fromPosition=${fromPosition}`).then((data) => {
                        if (data) {
                            resolve(data);
                        }
                    });
                });
                promises.push(promise);
            });
            return new Promise((resolve) => {
                Promise.all(promises).then(values => {
                    resolve(values);
                });
            });
        }
        return new Promise((resolve, reject) => {
            this.get(`vcf/${tracksVCF.id}/${chromosomeId}/prev/?fromPosition=${fromPosition}`).catch(() => {
                resolve([]);
            }).then((data) => {
                if (data) {
                    resolve(data);
                }
            });
        });
    }

}