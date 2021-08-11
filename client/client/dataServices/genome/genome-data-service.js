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
                .then((data) => {
                    if (data) {
                        resolve(data);
                    } else {
                        resolve({});
                    }
                })
                .catch(reject);
        });
    }

    _homologene = [];
    _homologeneResult = [];
    _orthoPara = [];
    _orthoParaResult = [];

    getHomologeneLoad() {
        const length = 100;
        if (!this._homologene.length) {
            for (let i = 0; i < length; i += 1) {
                this._homologene.push(this.getHomologeneMock());
            }
        }
        // return this._homologene;
        return [
            {
                gene: 'KRAS, Kras, kras, Ras85D, let-60',
                protein: 'v-Ki-ras2 Kirsten rat sarcoma viral oncogene homolog',
                info: 'Gene conserved in Bilateria'
            },
            {
                gene: 'SSPN, Sspn, sspn',
                protein: 'sarcospan',
                info: 'Gene conserved in Euteleostomi'
            }
        ];
    }

    getHomologeneResultLoad() {
        const length = 100;
        if (!this._homologeneResult.length) {
            for (let i = 0; i < length; i += 1) {
                this._homologeneResult.push(this.getHomologeneResultMock());
            }
        }
        // return this._homologeneResult;
        return [
            {
                name: 'KRAS',
                species: 'H.sapiens',
                accession_id: 'NP_203524.1',
                aa: 189,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'KRAS',
                species: 'P.troglodytes',
                accession_id: 'XP_003313842.1',
                aa: 189,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'KRAS',
                species: 'C.lupus',
                accession_id: 'XP_003433608.2',
                aa: 229,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'KRAS',
                species: 'B.taurus',
                accession_id: 'NP_001103471.1',
                aa: 188,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'Kras',
                species: 'M.musculus',
                accession_id: 'NP_067259.4',
                aa: 188,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'Kras',
                species: 'R.norvegicus',
                accession_id: 'NP_113703.1',
                aa: 188,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'KRAS',
                species: 'G.gallus',
                accession_id: 'NP_001243091.1',
                aa: 188,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'LOC100485959',
                species: 'X.tropicalis',
                accession_id: '\tXP_004920180.1',
                aa: 226,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'kras',
                species: 'D.rerio',
                accession_id: 'NP_001003744.1',
                aa: 188,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'Ras85D',
                species: 'D.melanogaster',
                accession_id: 'NP_476699.1',
                aa: 189,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'AgaP_AGAP002219',
                species: 'A.gambiae',
                accession_id: 'XP_307965.4',
                aa: 190,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
            {
                name: 'let-60',
                species: 'C.elegans',
                accession_id: 'NP_502213.3',
                aa: 184,
                domains: [{
                    id: 'cl17170',
                    start: 3,
                    end: 180,
                    name: 'Ras_like_GTPase'
                }]
            },
        ];
    }

    getOrthoParaLoad() {
        const length = 200;
        if (!this._orthoPara.length) {
            for (let i = 0; i < length; i += 1) {
                this._orthoPara.push(this.getHomologeneMock());
            }
        }
        // return this._orthoPara;

        return [
            {
                gene: 'KRAS, Kras, kras, Ras85D, let-60',
                protein: 'v-Ki-ras2 Kirsten rat sarcoma viral oncogene homolog',
                info: 'Gene conserved in Bilateria'
            },
            {
                gene: 'SSPN, Sspn, sspn',
                protein: 'sarcospan',
                info: 'Gene conserved in Euteleostomi'
            }
        ];
    }

    getOrthoParaResultLoad() {
        const length = 100;
        if (!this._orthoParaResult.length) {
            for (let i = 0; i < length; i += 1) {
                this._orthoParaResult.push(this.getOrthoParaResultMock());
            }
        }
        // return this._orthoParaResult;
        return [
            {
                name: 'KRAS',
                species: 'Homo sapiens',
                type: 'Ortholog',
                aa: 188,
                domains: [{
                    id: 'cd04138',
                    start: 3,
                    end: 164,
                    name: 'H_N_K_Ras_like'
                }]
            },
            {
                name: 'kras',
                species: 'Mus musculus',
                type: 'Ortholog',
                aa: 188,
                domains: [{
                    id: 'cd04138',
                    start: 3,
                    end: 164,
                    name: 'H_N_K_Ras_like'
                }]
            },
            {
                name: 'kras',
                species: 'Rattus norvegicus',
                type: 'Ortholog',
                aa: 227,
                domains: [{
                    id: 'cd04138',
                    start: 3,
                    end: 164,
                    name: 'H_N_K_Ras_like'
                }]
            },
            {
                name: 'KRAS',
                species: 'Canis lupus familiaris',
                type: 'Ortholog',
                aa: 188,
                domains: [{
                    id: 'cd04138',
                    start: 3,
                    end: 164,
                    name: 'H_N_K_Ras_like'
                }]
            },
            {
                name: 'KRAS',
                species: 'Gallus gallus',
                type: 'Ortholog',
                aa: 229,
                domains: [{
                    id: 'cd04138',
                    start: 50,
                    end: 164,
                    name: 'H_N_K_Ras_like'
                }]
            },
            {
                name: 'KRAS',
                species: 'Pan troglodytes',
                type: 'Ortholog',
                aa: 188,
                domains: [{
                    id: 'cd04138',
                    start: 3,
                    end: 164,
                    name: 'H_N_K_Ras_like'
                }]
            },
            {
                name: 'KRAS',
                species: 'Bos taurus',
                type: 'Paralog',
                aa: 189,
                domains: [{
                    id: 'cd04138',
                    start: 3,
                    end: 164,
                    name: 'H_N_K_Ras_like'
                }]
            },
        ];
    }

    getHomologeneMock() {
        return {
            gene: this.getRandomSentence(Math.round(5 * Math.random()) + 1, ', '),
            protein: this.getRandomSentence(Math.round(5 * Math.random()) + 1, ' '),
            info: this.getRandomString(Math.round(200 * Math.random()) + 50),
        };
    }

    getHomologeneResultMock() {
        const aa = Math.round(200 * Math.random()) + 50;
        return {
            name: this.getRandomString(Math.round(5 * Math.random()) + 3),
            species: this.getRandomString(Math.round(20 * Math.random()) + 5),
            accession_id: this.getRandomString(Math.round(20 * Math.random()) + 5),
            aa: aa,
            domains: this.getRandomDomains(Math.round(Math.random()) + 3, aa)
        };
    }

    getOrthoParaResultMock() {
        const aa = Math.round(200 * Math.random()) + 50;
        const types = ['Ortholog', 'Paralog'];
        return {
            name: this.getRandomString(Math.round(5 * Math.random()) + 3),
            species: this.getRandomString(Math.round(20 * Math.random()) + 5),
            type: types[Math.round(Math.random())],
            aa: aa,
            domains: this.getRandomDomains(Math.round(Math.random()) + 3, aa)
        };
    }

    getRandomSentence(length, separator) {
        let result = '';
        for (let i = 0; i < length; i += 1) {
            result += this.getRandomString(Math.round(10 * Math.random()) + 3) + separator;
        }
        return result.substring(0, result.length - 2);
    }

    getRandomString(length) {
        let result = '';
        const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-.';
        for (let i = 0; i < length; i += 1) {
            result += characters.charAt(Math.floor(Math.random() * characters.length));
        }
        return result;
    }

    getRandomDomains(length, end) {
        const domainName = ['A', 'B', 'C', 'D'];
        const result = [];
        const maxDomainLength = end / length / 1.5;
        let lastEnd = Math.round(maxDomainLength * Math.random() + 10),
            lastStart = Math.round(30 * Math.random());
        for (let i = 0; i < length; i += 1) {
            result.push({
                id: this.getRandomString(10),
                start: lastStart,
                end: lastEnd,
                name: domainName[Math.round((domainName.length - 1) * Math.random())]
            });
            lastStart = lastEnd + Math.round(30 * Math.random() + 10);
            lastEnd = Math.min(lastStart + Math.round(maxDomainLength * Math.random()), end);
        }
        return result;
    }

}
