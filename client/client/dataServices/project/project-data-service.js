import angular from 'angular';
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
                }
                else {
                    resolve([]);
                }
            });
        });
    }

    getProjectsFilterVcfInfo(vcfFileIds) {
        return new Promise((resolve, reject) => {
            this.post('filter/info', vcfFileIds).then(data => {
                if (data !== null && data !== undefined) {
                    resolve(data);
                }
                else {
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

    /**
     *
     * @param {number} referenceId
     * @param {string} featureId
     * @returns {promise}
     */
    searchGenes(referenceId, featureId) {
        return this.get(`reference/${referenceId}/search?featureId=${featureId}`);
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
            this.post('filter', filter).catch(()=>resolve([])).then((data) => {
                if (data) {
                    resolve(data);
                } else {
                    data = [];
                    resolve(data);
                }
            },reject);
        });
    }

    getVcfGroupData(filter, groupBy) {
        return new Promise((resolve, reject) => {
            this.post(`filter/group?groupBy=${groupBy}`, filter).catch((response)=>reject(response)).then((data) => {
                if (data) {
                    resolve(data);
                } else {
                    data = [];
                    resolve(data);
                }
            },reject);
        });
    }

    autocompleteGeneId(search, vcfIds) {
        return new Promise((resolve) => {
            this.post('filter/searchGenes', {search, vcfIds}).then((data) => {
                if (data) {
                    resolve(data);
                } else {
                    data = [];
                    resolve(data);
                }
            });
        });
    }
}