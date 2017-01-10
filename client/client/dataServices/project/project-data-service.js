import angular from 'angular';
import {DataService} from '../data-service';
/**
 * data service for project
 * @extends DataService
 */
export class ProjectDataService extends DataService {
    /**
     *
     * @returns {promise}
     */
    getServerProjects() {
        return new Promise((resolve, reject) => {
            this.get('project/loadMy').then(data => {
                if (data !== null && data !== undefined) {
                    resolve(data);
                }
                else {
                    reject();
                }
            });
        });
    }

    getProjects() {
        return new Promise((resolve, reject) => {
            this.get('project/tree').catch(() => resolve([])).then(data => {
                if (data !== null && data !== undefined) {
                    resolve(data);
                }
                else {
                    reject();
                }
            });
        });
    }

    getProjectsFilterVcfInfo(projectId) {
        return this.get(`project/${projectId}/filter/vcf/info`);
    }

    getActiveTracks(projectId, tracksSettings) {

        return new Promise(resolve => {
            this.getProject(projectId).then(project => resolve(filterItems(project.items)
                .map(m => {
                    m.hidden = false;
                    return m;
                })
                .sort(sortItems)
            ));
        });

        function sortItems(file1, file2) {
            if (!tracksSettings) return 0;
            const index1 = tracksSettings.findIndex(fundIndex(file1.bioDataItemId));
            const index2 = tracksSettings.findIndex(fundIndex(file2.bioDataItemId));

            if (index1 === index2)return 0;
            return index1 > index2 ? 1 : -1;

            function fundIndex(bioDataItemId) {
                return (element) => element.bioDataItemId.toString() === bioDataItemId.toString();
            }
        }


        function filterItems(projectFiles) {
            if (!tracksSettings) return projectFiles;

            return projectFiles.filter(file => {
                const [fileSettings ]= tracksSettings.filter(m => m.bioDataItemId.toString() === file.bioDataItemId.toString());
                return fileSettings && fileSettings.hidden !== true;
            });
        }

    }

    /**
     *
     * @param {number} projectId
     * @returns {promise}
     */
    getProject(projectId) {
        return this.get(`project/${projectId}/load`);
    }

    /**
     *
     * @param {number} projectId
     * @param {string} featureId
     * @returns {promise}
     */
    searchGenes(projectId, featureId) {
        return this.get(`project/${projectId}/search?featureId=${featureId}`);
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

    /**
     *
     * @param filter
     * @returns {promise}
     */
    vcfDefaultVariationsData = null;
    defaultFilter = null;

    getVcfVariationLoad(filter) {
        const {
            vcfFileIds,
            exon,
            chromosomeId,
            projectId,
            genes,
            variationTypes,
            additionalFilters,
            quality,
            infoFields
        }=filter;
        if (!angular.isDefined(filter.vcfFileIds)
            && !angular.isDefined(filter.exons)
            && !angular.isDefined(filter.chromosomeId)
            && !angular.isDefined(filter.genes)
            && !angular.isDefined(filter.variationTypes)
            && !angular.isDefined(filter.additionalFilters)
            && !angular.isDefined(filter.quality)
            && !angular.isDefined(filter.infoFields)) {
            if (this.defaultFilter !== null) {
                if (this.defaultFilter.projectId === filter.projectId) {
                    return this.vcfDefaultVariationsData;
                } else {
                    const tmp = new Promise((resolve, reject) => {
                        this.post(`project/${projectId}/filter/vcf`, {
                            chromosomeId,
                            exon,
                            vcfFileIds,
                            genes,
                            variationTypes,
                            additionalFilters,
                            quality,
                            infoFields
                        }).catch(()=>resolve([])).then((data) => {
                            if (data) {
                                resolve(data);
                            } else {
                                data = [];
                                resolve(data);
                            }
                        },reject);
                    });
                    this.vcfDefaultVariationsData = tmp;
                    this.defaultFilter = filter;
                    return tmp;
                }
            } else {
                const tmp = new Promise((resolve, reject) => {
                    this.post(`project/${projectId}/filter/vcf`, {
                        chromosomeId, exon, vcfFileIds, genes, variationTypes, additionalFilters, quality, infoFields
                    }).catch(()=>resolve([])).then((data) => {
                        if (data) {
                            resolve(data);
                        } else {
                            data = [];
                            resolve(data);
                        }
                    },reject);
                });
                this.vcfDefaultVariationsData = tmp;
                this.defaultFilter = filter;
                return tmp;
            }
        } else {
            return new Promise((resolve, reject) => {
                this.post(`project/${projectId}/filter/vcf`, {
                    chromosomeId, exon, vcfFileIds, genes, variationTypes, additionalFilters, quality, infoFields
                }).catch(()=>resolve([])).then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        data = [];
                        resolve(data);
                    }
                },reject);
            });
        }
    }

    autocompleteGeneId(projectId, search, vcfIds) {
        return new Promise((resolve) => {
            this.post(`project/${projectId}/filter/vcf/searchGenes`, {search, vcfIds}).then((data) => {
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