import {DataService} from '../data-service';

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

    postAssociatedDrugs(request) {
        return new Promise((resolve, reject) => {
            this.post('target/associated/drugs', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting associated drugs';
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    postAssociatedDiseases(request, source) {
        return new Promise((resolve, reject) => {
            this.post(`target/associated/diseases?source=${source}`, request)
                .then(data => {
                    if (data && data.length) {
                        resolve([data, data.length]);
                    } else {
                        resolve([[], 0]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting associated diseases';
                    reject(new Error((error && error.message) || message));
                });
        });
    }
}
