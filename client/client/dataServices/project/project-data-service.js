import {DataService} from '../data-service';

/**
 * data service for project
 * @extends DataService
 */
export class ProjectDataService extends DataService {

    getProjects(referenceName) {
        return new Promise((resolve) => {
            const url = referenceName ? `project/tree?referenceName=${referenceName}` : 'project/tree';
            this.get(url).catch(() => resolve([])).then(data => {
                if (data !== null && data !== undefined) {
                    resolve(data);
                } else {
                    resolve([]);
                }
            });
        });
    }

    getProjectsFilterVcfInfo(vcfFileIdsByProject) {
        return new Promise((resolve) => {
            this.post('filter/info', vcfFileIdsByProject).then(data => {
                if (data !== null && data !== undefined) {
                    resolve(data);
                } else {
                    resolve(null);
                }
            });
        });
    }

    /**
     *
     * @param {number} projectId
     * @returns {promise}
     */
    getProject(projectId) {
        if (isNaN(projectId)) {
            return null;
        }
        return this.get(`project/${+projectId}/load`);
    }

    getProjectIdDescription(projectId) {
        return this.downloadFile(`project/${+projectId}/description`, {customResponseType: 'arraybuffer'});
    }

    /**
     *
     * @param {number} referenceId
     * @param {string} featureId
     * @returns {promise}
     */
    searchGenes(referenceId, featureId) {
        return this.get(`reference/${referenceId}/search?featureId=${featureId}`);
    }

    searchGeneNames(referenceId, featureId) {
        return new Promise((resolve) => {
            this.get(`reference/${referenceId}/search?featureId=${featureId}`).then((data) => {
                if (data && data.entries) {
                    resolve(data.entries.map(e => e.featureName));
                } else {
                    resolve([]);
                }
            });
        });
    }

    /**
     *
     * @param {Project} project
     * @returns {promise}
     */
    saveProject(project) {
        return new Promise((resolve, reject) => {
            this.post('project/save', project).then((data) => {
                if (data) {
                    resolve(data);
                } else {
                    reject('ERROR on saving project');
                }
            });
        });
    }

    deleteProject(projectId) {
        return new Promise((resolve) => {
            this.delete(`project/${projectId}`).then((data) => {
                resolve(data);
            });
        });
    }

    getVcfVariationLoad(filter) {
        return new Promise((resolve, reject) => {
            this.post('filter', filter).catch((response) => resolve({...response, error: true})).then((data) => {
                if (data) {
                    resolve(data);
                } else {
                    data = [];
                    resolve(data);
                }
            }, reject);
        });
    }

    getVcfGroupData(filter, groupBy) {
        return new Promise((resolve, reject) => {
            this.post(`filter/group?groupBy=${groupBy}`, filter).catch((response) => reject(response)).then((data) => {
                if (data) {
                    resolve(data);
                } else {
                    data = [];
                    resolve(data);
                }
            }, reject);
        });
    }

    getOrganismList(term) {
        return new Promise((resolve, reject) => {
            this.get(`taxonomies/${term}`)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    getBlastDBList(type) {
        return new Promise((resolve, reject) => {
            this.get(`blast/databases?type=${type}`)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    getBlastHistoryLoad(filter) {
        return new Promise((resolve, reject) => {
            this.post('tasks', filter)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    getBlastSearch(searchId) {
        return new Promise((resolve, reject) => {
            this.get(`task/${searchId}`)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    getBlastResultLoad(searchId) {
        return new Promise((resolve, reject) => {
            this.get(`task/${searchId}/group`)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    createBlastSearch(blastSearch) {
        return new Promise((resolve, reject) => {
            this.post('task', blastSearch)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    deleteBlastSearchHistory() {
        return new Promise((resolve, reject) => {
            this.delete('tasks')
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    cancelBlastSearch(blastSearchId) {
        return new Promise((resolve, reject) => {
            this.put(`task/${blastSearchId}/cancel`)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    downloadBlastResults(blastSearchId) {
        return new Promise((resolve, reject) => {
            this.downloadFile(`task/${blastSearchId}/raw`, {customResponseType: 'arraybuffer'})
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                }, reject);
        });
    }

    getDatasetFileLink(bioDataItemId) {
        return this.getFullUrl(`dataitem/${bioDataItemId}/download`);
    }

    getCoordsUrl(searchResult, search) {
        const {sequenceAccessionVersion, sequenceId, taxId} = searchResult;
        const {dbType} = search;
        const id = sequenceAccessionVersion || sequenceId;
        const db = search && dbType && /^protein$/i.test(dbType)
            ? 'PROTEIN'
            : 'NUCLEOTIDE';
        return id && db && taxId ? `blast/coordinate?sequenceId=${id}&type=${db}&taxId=${taxId}`: undefined;
    }

    getNCBIFeatureCoordinates(searchResult, search) {
        const requestUrl = this.getCoordsUrl(searchResult, search);
        return new Promise((resolve, reject) => {
            this.get(requestUrl)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = {};
                        resolve(data);
                    }
                }, reject);
        });
    }
    getDatasetFileInfo(bioDataItemId) {
        return new Promise((resolve, reject) => {
            this.get(`dataitem/${bioDataItemId}/downloadUrl`)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = {};
                        resolve(data);
                    }
                }, reject);
        });
    }
}
