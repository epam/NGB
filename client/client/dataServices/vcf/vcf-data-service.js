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
        let url = 'vcf/variation/load';
        if (variantData.openByUrl) {
            url = `vcf/variation/load?fileUrl=${encodeURIComponent(variantData.fileUrl)}&indexUrl=${encodeURIComponent(variantData.indexUrl)}`;
            variantData.openByUrl = undefined;
            variantData.fileUrl = undefined;
            variantData.indexUrl = undefined;
            variantData.projectId = undefined;
        }
        return new Promise((resolve) => {
            this.post(url, variantData)
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
            let url = 'vcf/track/get';
            if (vcf.openByUrl) {
                url = `vcf/track/get?fileUrl=${encodeURIComponent(vcf.fileUrl)}&indexUrl=${encodeURIComponent(vcf.indexUrl)}`;
                vcf.openByUrl = undefined;
                vcf.fileUrl = undefined;
                vcf.indexUrl = undefined;
            }
            this.post(url, vcf).then((data) => {
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
                    let url = `vcf/${chromosomeId}/next?trackId=${trackVCF.id}&fromPosition=${fromPosition}`;
                    if (trackVCF.openByUrl) {
                        url = `vcf/${chromosomeId}/next?fileUrl=${encodeURIComponent(trackVCF.id)}&indexUrl=${encodeURIComponent(trackVCF.indexPath)}&fromPosition=${fromPosition}`;
                    }
                    this.get(url).then((data) => {
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
        let url = `vcf/${chromosomeId}/next?trackId=${tracksVCF.id}&fromPosition=${fromPosition}`;
        if (tracksVCF.openByUrl) {
            url = `vcf/${chromosomeId}/next?fileUrl=${encodeURIComponent(tracksVCF.id)}&indexUrl=${encodeURIComponent(tracksVCF.indexPath)}&fromPosition=${fromPosition}`;
        }
        return new Promise((resolve, reject) => {
            this.get(url).catch(() => {
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
                    let url = `vcf/${chromosomeId}/prev?trackId=${trackVCF.id}&fromPosition=${fromPosition}`;
                    if (trackVCF.openByUrl) {
                        url = `vcf/${chromosomeId}/prev?fileUrl=${encodeURIComponent(trackVCF.id)}&indexUrl=${encodeURIComponent(trackVCF.indexPath)}&fromPosition=${fromPosition}`;
                    }
                    this.get(url).then((data) => {
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
        let url = `vcf/${chromosomeId}/prev?trackId=${tracksVCF.id}&fromPosition=${fromPosition}`;
        if (tracksVCF.openByUrl) {
            url = `vcf/${chromosomeId}/prev?fileUrl=${encodeURIComponent(tracksVCF.id)}&indexUrl=${encodeURIComponent(tracksVCF.indexPath)}&fromPosition=${fromPosition}`;
        }
        return new Promise((resolve, reject) => {
            this.get(url).catch(() => {
                resolve([]);
            }).then((data) => {
                if (data) {
                    resolve(data);
                }
            });
        });
    }

}