import {DataService} from '../data-service';

const SOURCE = {
    OPEN_TARGETS: 'OPEN_TARGETS',
    TXGNN: 'TXGNN',
    DGI_DB: 'DGI_DB',
    PHARM_GKB: 'PHARM_GKB'
};

export class TargetDataService extends DataService {

    getTargetsResult(request) {
        return new Promise((resolve, reject) => {
            this.post('target/filter', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error fetching targets';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    postNewTarget(request) {
        return new Promise((resolve, reject) => {
            this.post('target', request)
                .then(data => {
                    if (data && data.targetId) {
                        resolve(data);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    const message = 'Error creating target';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getTargetById(targetId) {
        return new Promise((resolve) => {
            this.get(`target/${targetId}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                });
        });
    }

    updateTarget(request) {
        return new Promise((resolve, reject) => {
            this.put('target', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    }
                })
                .catch(error => {
                    const message = 'Error updating target';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    deleteTarget(targetId) {
        return new Promise((resolve, reject) => {
            this.delete(`target/${targetId}`)
                .then((data) => {
                    resolve(data || null);
                })
                .catch((error) => {
                    const message = 'Error removing target';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getTargetFieldValue(field) {
        return new Promise((resolve) => {
            this.get(`target/fieldValues?field=${field}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                });
        });
    }

    searchGenes(geneId) {
        return new Promise((resolve) => {
            this.get(`gene/search?geneId=${geneId}`)
                .then(data => {
                    if (data && data.entries) {
                        resolve(data.entries);
                    } else {
                        resolve([]);
                    }
                });
        });
    }

    postTargetIdentification(request) {
        return new Promise((resolve, reject) => {
            this.post('target/identification', request)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve({});
                    }
                })
                .catch(error => {
                    const message = 'Error launching target';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDrugsResults(request, source) {
        if (source === SOURCE.OPEN_TARGETS) {
            return this.getOpenTargetsDrugs(request);
        }
        if (source === SOURCE.PHARM_GKB) {
            return this.getPharmGKBDrugs(request);
        }
        if (source === SOURCE.DGI_DB) {
            return this.getDGIdbDrugs(request);
        }
    }

    getOpenTargetsDrugs(request) {
        return new Promise((resolve, reject) => {
            this.post('target/opentargets/drugs', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting drugs from Open Targets';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getPharmGKBDrugs(request) {
        return new Promise((resolve, reject) => {
            this.post('target/pharmgkb/drugs', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting drugs from PharmGKB';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDGIdbDrugs(request) {
        return new Promise((resolve, reject) => {
            this.post('target/dgidb/drugs', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting drugs from DGIdb';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDiseasesResults(request, source) {
        if (source === SOURCE.OPEN_TARGETS) {
            return this.getOpenTargetsDiseases(request);
        }
        if (source === SOURCE.PHARM_GKB) {
            return this.getPharmGKBDiseases(request);
        }
    }

    getOpenTargetsDiseases(request) {
        return new Promise((resolve, reject) => {
            this.post('target/opentargets/diseases', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting diseases from Open Targets';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getPharmGKBDiseases(request) {
        return new Promise((resolve, reject) => {
            this.post('target/pharmgkb/diseases', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting diseases from PharmGKB';
                    reject(new Error((error && error.message) || message));
                });
        });
    }
}
