import {DataService} from '../data-service';

/**
 * data service for genome
 * @extends DataService
 */
export class GenomeDataService extends DataService {
    /**
     * Returns all chromosomes for a reference genome associated with the given ID
     * @returns {promise}
     */
    loadAllChromosomes(referenceId) {
        return this.get(`reference/${referenceId}/loadChromosomes`);
    }

    /**
     * Returns all reference genomes that are available in the system at the moment.
     * @returns {promise}
     */
    loadAllReference(referenceName) {
        const url = referenceName ? `reference/loadAll?referenceName=${referenceName}` : 'reference/loadAll';
        return this.get(url);
    }

    loadReferenceTrack(reference) {
        if (reference instanceof Array) {
            const promises = [];
            for (let i = 0; i < reference.length; i++) {
                const promise = new Promise((resolve, reject) => {
                    this.post('reference/track/get', reference[i])
                        .then((data) => {
                            if (data) {
                                resolve(data);
                            } else {
                                const code = 'Genome Data Service', message = 'Genome Data Service: error loading reference track';
                                reject({code, message});
                            }
                        })
                        .catch(error => reject({message: error.message, code: 'Genome Data Service'}));
                });
                promises.push(promise);
            }
            return new Promise((resolve) => {
                Promise.all(promises)
                    .then(values => {
                        let data = null;
                        for (let i = 0; i < values.length; i++) {
                            if (!data) {
                                data = values[i];
                            } else {
                                for (let j = 0; j < values[i].blocks.length; j++) {
                                    data.blocks.push(values[i].blocks[j]);
                                }
                            }
                        }
                        resolve(data);
                    })
                    .catch((e) => {
                        // eslint-disable-next-line
                        console.warn(e);
                        resolve([]);
                    });
            });
        }
        return new Promise((resolve, reject) => {
            this.post('reference/track/get', reference).catch(() => {
                resolve({
                    blocks: []
                });
            }).then((data) => {
                if (data) {
                    resolve(data);
                } else {
                    const code = 'Genome Data Service', message = 'Genome Data Service: error loading reference track';
                    reject({code, message});
                }
            });
        });
    }

    loadChromosome(chromosomeId) {
        return this.get(`reference/chromosomes/${chromosomeId}/load`);
    }

    loadChromosomeByName(referenceId, name) {
        return new Promise((resolve, reject) => {
            this.loadAllChromosomes(referenceId).catch(() => {
                resolve([]);
            }).then((data) => {
                if (data) {
                    const chromosomesAreEqual = (chr1, chr2) => {
                        if (!chr1 || !chr2)
                            return false;
                        chr1 = chr1.toLowerCase();
                        chr2 = chr2.toLowerCase();
                        if (chr1.startsWith('chr')) {
                            chr1 = chr1.substr(3);
                        }
                        if (chr2.startsWith('chr')) {
                            chr2 = chr2.substr(3);
                        }
                        return chr1 === chr2;
                    };
                    const [chromosome] = data.filter(chr => chromosomesAreEqual(chr.name, `${name}`));
                    resolve(chromosome);
                } else {
                    const code = 'Genome Data Service', message = 'Genome Data Service: error loading chromosomes';
                    reject({code, message});
                }
            });
        });
    }

    loadGenes(reference, filter) {
        return new Promise((resolve, reject) => {
            this.post(`reference/${reference}/filter/gene`, filter)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No genes received'));
                    }
                })
                .catch(reject);
        });
    }

    getGenesInfo(reference) {
        return new Promise((resolve, reject) => {
            this.post(`reference/${reference}/filter/gene/info`)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No gene info received'));
                    }
                })
                .catch(reject);
        });
    }

    filterGeneValues(reference, fieldName, filter = {}) {
        return new Promise((resolve, reject) => {
            this.post(`reference/${reference}/filter/gene/values?fieldName=${fieldName}`, filter)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                })
                .catch(reject);
        });
    }

    getGeneFeatureTypes(referenceId, projectId, geneFileId) {
        return this.filterGeneValues(
            referenceId,
            'featureType',
            {
                value: {[projectId]: [geneFileId]}
            }
        );
    }

    downloadGenes(reference, getParams, data) {
        return new Promise((resolve, reject) => {
            this.downloadFile('post', `reference/${reference}/filter/gene/export?format=${getParams.format}&includeHeader=${getParams.includeHeader}`,
                data, {customResponseType: 'arraybuffer'})
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

    getGeneInfo(geneFileId, uid) {
        return new Promise((resolve, reject) => {
            this.get(`gene/${geneFileId}/doc?uid=${uid}`)
                .catch((response) => resolve({...response, error: true}))
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve({});
                    }
                }, reject);
        });
    }

    getHomologeneLoad(filter) {
        return new Promise((resolve, reject) => {
            this.post('homologene/search', filter)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No homologene received'));
                    }
                })
                .catch(reject);
        });
    }

    _internalPathways = {
        items: [
            {
                name: 'activated_stat1alpha_induction_of_the_irf1_gene',
                description: 'desc1',
                xml: 'activated_stat1alpha_induction_of_the_irf1_gene'
            },
            {
                name: 'Glycolysis',
                description: 'desc2',
                xml: 'Glycolysis'
            },
        ],
        totalCount: 2
    };

    getOrthoParaLoad(filter) {
        return new Promise((resolve, reject) => {
            this.post('homolog/search', filter)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No orthologs or paralogs received'));
                    }
                })
                .catch(reject);
        });
    }

    getSpeciesList() {
        return new Promise((resolve, reject) => {
            this.get('pathway/species')
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve([]);
                    }
                })
                .catch(reject);
        });
    }

    getInternalPathwaysLoad(filter) {
        // return this._internalPathways;
        return new Promise((resolve, reject) => {
            this.post('pathways', filter)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No pathways received'));
                    }
                })
                .catch(reject);
        });
    }

    loadPathwayFileById(id) {
        return new Promise((resolve, reject) => {
            this.getRawFile('get', `pathway/content/${id}`, null, {customResponseType: 'text'})
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No pathway info received'));
                    }
                })
                .catch(reject);
        });
    }

    getAllLineageTrees() {
        return new Promise((resolve, reject) => {
            this.get('lineage/trees/all')
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No lineage trees received'));
                    }
                })
                .catch(reject);
        });
    }

    getLineageTreesByReference(referenceId) {
        return new Promise((resolve, reject) => {
            this.get(`lineage/trees/${referenceId}`)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No lineage trees received'));
                    }
                })
                .catch(reject);
        });
    }

    getLineageTreeById(id) {
        return new Promise((resolve, reject) => {
            this.get(`lineage/tree/${id}`)
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        reject(new Error('No data received'));
                    }
                })
                .catch(reject);
        });
    }
}
