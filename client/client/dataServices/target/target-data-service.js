import {DataService} from '../data-service';

const SOURCE = {
    OPEN_TARGETS: 'OPEN_TARGETS',
    TXGNN: 'TXGNN',
    DGI_DB: 'DGI_DB',
    PHARM_GKB: 'PHARM_GKB'
};

const ExternalDBApi = {
    [SOURCE.OPEN_TARGETS]: 'opentargets',
    [SOURCE.TXGNN]: 'txgnn',
    [SOURCE.DGI_DB]: 'dgidb',
    [SOURCE.PHARM_GKB]: 'pharmgkb'
};

const ExternalDBNames = {
    [SOURCE.OPEN_TARGETS]: 'Open Targets',
    [SOURCE.TXGNN]: 'TxGNN',
    [SOURCE.DGI_DB]: 'DGIdb',
    [SOURCE.PHARM_GKB]: 'PharmGKB'
}

const ExternalDBFields = {
    [SOURCE.OPEN_TARGETS]: 'opentargets',
    [SOURCE.DGI_DB]: 'dgidb',
    [SOURCE.PHARM_GKB]: 'pharmGKB'
}

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
        return new Promise((resolve, reject) => {
            this.post(`target/${ExternalDBApi[source]}/drugs`, request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = `Error getting drugs from ${ExternalDBNames[source]}`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getDiseasesResults(request, source) {
        return new Promise((resolve, reject) => {
            this.post(`target/${ExternalDBApi[source]}/diseases`, request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], data.totalCount]);
                    }
                })
                .catch(error => {
                    const message = `Error getting diseases from ${ExternalDBNames[source]}`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getAllDiseasesResults(request, source) {
        return new Promise((resolve, reject) => {
            this.post(`target/${ExternalDBApi[source]}/diseases/all`, request)
                .then(data => resolve(data || []))
                .catch(error => {
                    const message = `Error getting all diseases from ${ExternalDBNames[source]}`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getOntology(source) {
        return new Promise((resolve, reject) => {
            this.get(`target/${ExternalDBApi[source]}/diseases/ontology`)
                .then(data => resolve(data || []))
                .catch(error => {
                    const message = `Error getting diseases ontology from ${ExternalDBNames[source]}`;
                    reject(new Error((error && error.message) || message));
                });
        });
    }

    getPublications(request) {
        return new Promise((resolve, reject) => {
            this.post('target/publications', request)
                .then(data => {
                    if (data && data.items) {
                        resolve([data.items, data.totalCount]);
                    } else {
                        resolve([[], 0]);
                    }
                })
                .catch(error => {
                    const message = 'Error getting publications';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getLlmSummary(request, provider) {
        return new Promise((resolve, reject) => {
            this.post(`llm/summary?provider=${provider}`, request)
                .then(data => {
                    resolve(data);
                })
                .catch(error => {
                    const message = 'Error generating publications summary';
                    reject(new Error((error && error.message) || message));
            });
        });
    }

    getDrugsFieldValues(source, geneIds) {
        return new Promise((resolve) => {
            this.get(`target/${ExternalDBFields[source]}/drugs/fieldValues?geneIds=${geneIds}`)
                .then(data => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                });
        });
    }
}
